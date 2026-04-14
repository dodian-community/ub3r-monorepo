package net.dodian.uber.game.engine.systems.interaction.objects

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.api.plugin.ContentModuleIndex
import net.dodian.uber.game.objects.ObjectBinding
import net.dodian.uber.game.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.api.content.ContentInteractionType
import net.dodian.uber.game.api.content.ContentObjectInteractionPolicy
import net.dodian.uber.game.api.plugin.ContentBootstrap
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

object ObjectContentRegistry : ContentBootstrap {
    override val id: String = "objects.registry"
    private val logger = LoggerFactory.getLogger(ObjectContentRegistry::class.java)

    private data class RegisteredBinding(
        val moduleName: String,
        val content: ObjectContent,
        val binding: ObjectBinding,
    )

    data class ObjectResolution(
        val moduleName: String,
        val content: ObjectContent,
        val binding: ObjectBinding,
        val bindingKey: String,
    )

    private val bootstrapped = AtomicBoolean(false)
    private val definitions = mutableListOf<Pair<String, ObjectContent>>()

    @Volatile
    private var byObjectId: Array<Array<RegisteredBinding>?> = emptyArray()

    private val resolutionComparator = compareByDescending<RegisteredBinding> { it.binding.matcher.specificity }
        .thenByDescending { it.binding.priority }
        .thenBy { it.moduleName }
        .thenBy { it.binding.matcher.describe() }

    override fun bootstrap() {
        if (bootstrapped.get()) return
        synchronized(this) {
            if (bootstrapped.get()) return
            definitions += ContentModuleIndex.objectContents
            rebuildIndexLocked()
            bootstrapped.set(true)
        }
    }

    fun ensureLoaded() = bootstrap()

    fun register(content: ObjectContent) = register(content::class.simpleName ?: "ObjectContent", content)

    fun register(name: String, content: ObjectContent) {
        synchronized(this) {
            definitions += name to content
            if (bootstrapped.get()) {
                rebuildIndexLocked()
            }
        }
    }

    @JvmStatic
    fun resolve(objectId: Int, position: Position): ObjectContent? {
        return resolveCandidates(objectId, position).firstOrNull()?.content
    }

    @JvmStatic
    fun resolveAll(objectId: Int, position: Position): List<ObjectContent> {
        return resolveCandidates(objectId, position)
            .map { it.content }
            .distinctBy { it::class.java.name }
    }

    @JvmStatic
    fun resolveCandidates(objectId: Int, position: Position): List<ObjectResolution> {
        bootstrap()
        return byObjectId.getOrNull(objectId)
            .orEmpty()
            .asSequence()
            .filter { it.binding.matcher.matches(position) }
            .map {
                ObjectResolution(
                    moduleName = it.moduleName,
                    content = it.content,
                    binding = it.binding,
                    bindingKey = "${it.moduleName}:${it.binding.objectId}:${it.binding.matcher.describe()}:${it.binding.priority}",
                )
            }
            .toList()
    }

    @JvmStatic
    fun resolvePolicy(
        objectId: Int,
        position: Position,
        interactionType: ContentInteractionType,
        option: Int = -1,
        obj: GameObjectData? = null,
        itemId: Int = -1,
        itemSlot: Int = -1,
        interfaceId: Int = -1,
        spellId: Int = -1,
    ): ContentObjectInteractionPolicy? {
        bootstrap()
        val bucket = byObjectId.getOrNull(objectId).orEmpty()
        for (entry in bucket) {
            if (!entry.binding.matcher.matches(position)) {
                continue
            }
            val policy =
                when (interactionType) {
                    ContentInteractionType.CLICK ->
                        entry.content.clickInteractionPolicy(option, objectId, position, obj)
                    ContentInteractionType.ITEM_ON_OBJECT ->
                        entry.content.itemOnObjectInteractionPolicy(
                            objectId = objectId,
                            position = position,
                            obj = obj,
                            itemId = itemId,
                            itemSlot = itemSlot,
                            interfaceId = interfaceId,
                        )
                    ContentInteractionType.MAGIC ->
                        entry.content.magicOnObjectInteractionPolicy(
                            objectId = objectId,
                            position = position,
                            obj = obj,
                            spellId = spellId,
                        )
                }
            if (policy != null) {
                return policy
            }
        }
        return null
    }

    @JvmStatic
    fun prewarmObjectDefinitions() {
        bootstrap()
        val snapshot = byObjectId
        for (objectId in snapshot.indices) {
            if (snapshot[objectId] != null) {
                GameObjectData.forId(objectId)
            }
        }
    }

    @JvmStatic
    fun get(objectId: Int): ObjectContent? {
        bootstrap()
        return byObjectId.getOrNull(objectId)?.firstOrNull()?.content
    }

    internal fun bindingsForObjectForTests(objectId: Int): List<ObjectBinding> {
        return byObjectId.getOrNull(objectId).orEmpty().map { it.binding }
    }

    internal fun clearForTests() {
        bootstrapped.set(true)
        byObjectId = emptyArray()
        definitions.clear()
    }

    internal fun resetForTests() {
        bootstrapped.set(false)
        byObjectId = emptyArray()
        definitions.clear()
    }

    private fun rebuildIndexLocked() {
        val registered =
            definitions
                .flatMap { (name, content) ->
                    val bindings = content.bindings()
                    val registeredBindings = bindings.map { RegisteredBinding(name, content, it) }
                    validateInternalOverlaps(name, registeredBindings)
                    if (bindings.isNotEmpty()) {
                        logger.debug(
                            "Registered ObjectContent {} with {} bindings and objectIds={}",
                            name,
                            bindings.size,
                            bindings.map { it.objectId }.distinct().sorted().joinToString(","),
                        )
                    } else {
                        logger.debug("Registered ObjectContent {} with no bindings", name)
                    }
                    registeredBindings
                }
                .sortedWith(compareBy({ it.binding.objectId }, { it.binding.matcher.describe() }))

        val maxObjectId = registered.maxOfOrNull { it.binding.objectId } ?: -1
        val buckets = arrayOfNulls<MutableList<RegisteredBinding>>(maxObjectId + 1)
        for (entry in registered) {
            val objectId = entry.binding.objectId
            val bucket = buckets[objectId] ?: ArrayList<RegisteredBinding>().also { buckets[objectId] = it }
            validateCrossModuleOverlap(entry, bucket)
            bucket += entry
        }
        val rebuilt = arrayOfNulls<Array<RegisteredBinding>>(maxObjectId + 1)
        for (objectId in buckets.indices) {
            val bucket = buckets[objectId] ?: continue
            rebuilt[objectId] = bucket.sortedWith(resolutionComparator).toTypedArray()
        }
        byObjectId = rebuilt
    }

    private fun validateInternalOverlaps(name: String, entries: List<RegisteredBinding>) {
        val grouped = entries.groupBy { it.binding.objectId }
        for ((objectId, bucket) in grouped) {
            for (i in 0 until bucket.size) {
                for (j in i + 1 until bucket.size) {
                    val left = bucket[i]
                    val right = bucket[j]
                    if (!coexistAllowed(left.binding, right.binding)) {
                        throw IllegalArgumentException(
                            "Overlapping object bindings in $name for objectId=$objectId " +
                                "(${left.binding.matcher.describe()} vs ${right.binding.matcher.describe()})",
                        )
                    }
                }
            }
        }
    }

    private fun validateCrossModuleOverlap(candidate: RegisteredBinding, existing: List<RegisteredBinding>) {
        for (bound in existing) {
            if (!coexistAllowed(candidate.binding, bound.binding)) {
                throw IllegalArgumentException(
                    "Overlapping object bindings for objectId=${candidate.binding.objectId}: " +
                        "${candidate.moduleName}(${candidate.binding.matcher.describe()}) conflicts with " +
                        "${bound.moduleName}(${bound.binding.matcher.describe()})",
                )
            }
        }
    }

    private fun coexistAllowed(left: ObjectBinding, right: ObjectBinding): Boolean {
        if (!left.matcher.overlaps(right.matcher)) {
            return true
        }
        if (left.matcher.specificity != right.matcher.specificity) {
            return true
        }
        // Allow deterministic layered fallback when priority differs.
        return left.priority != right.priority
    }
}
