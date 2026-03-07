package net.dodian.uber.game.content.objects

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.runtime.interaction.ObjectInteractionPolicy
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

object ObjectContentRegistry {
    private val logger = LoggerFactory.getLogger(ObjectContentRegistry::class.java)

    private data class RegisteredBinding(
        val moduleName: String,
        val content: ObjectContent,
        val binding: ObjectBinding,
    )

    private val bootstrapped = AtomicBoolean(false)
    private val definitions = mutableListOf<Pair<String, ObjectContent>>()

    @Volatile
    private var byObjectId: Array<Array<RegisteredBinding>?> = emptyArray()

    private val resolutionComparator = compareByDescending<RegisteredBinding> { it.binding.matcher.specificity }
        .thenByDescending { it.binding.priority }
        .thenBy { it.moduleName }
        .thenBy { it.binding.matcher.describe() }

    @JvmStatic
    fun bootstrap() {
        if (bootstrapped.get()) return
        synchronized(this) {
            if (bootstrapped.get()) return
            definitions += builtinDefinitions()
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
        bootstrap()
        val bucket = byObjectId.getOrNull(objectId) ?: return null
        return bucket.firstOrNull { it.binding.matcher.matches(position) }?.content
    }

    @JvmStatic
    fun resolveAll(objectId: Int, position: Position): List<ObjectContent> {
        bootstrap()
        val resolved = byObjectId.getOrNull(objectId)
            .orEmpty()
            .asSequence()
            .filter { it.binding.matcher.matches(position) }
            .map { it.content }
            .toList()
        return resolved.distinctBy { it::class.java.name }
    }

    @JvmStatic
    fun resolvePolicy(
        objectId: Int,
        position: Position,
        interactionType: ObjectInteractionPolicy.InteractionType,
        option: Int = -1,
        obj: GameObjectData? = null,
        itemId: Int = -1,
        itemSlot: Int = -1,
        interfaceId: Int = -1,
        spellId: Int = -1,
    ): ObjectInteractionPolicy? {
        bootstrap()
        val bucket = byObjectId.getOrNull(objectId).orEmpty()
        for (entry in bucket) {
            if (!entry.binding.matcher.matches(position)) {
                continue
            }
            val policy =
                when (interactionType) {
                    ObjectInteractionPolicy.InteractionType.CLICK ->
                        entry.content.clickInteractionPolicy(option, objectId, position, obj)
                    ObjectInteractionPolicy.InteractionType.ITEM_ON_OBJECT ->
                        entry.content.itemOnObjectInteractionPolicy(
                            objectId = objectId,
                            position = position,
                            obj = obj,
                            itemId = itemId,
                            itemSlot = itemSlot,
                            interfaceId = interfaceId,
                        )
                    ObjectInteractionPolicy.InteractionType.MAGIC ->
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

    private fun builtinDefinitions(): List<Pair<String, ObjectContent>> =
        listOf(
            "AltarObjects" to net.dodian.uber.game.content.objects.impl.prayer.AltarObjects,
            "AnvilObjects" to net.dodian.uber.game.content.objects.impl.smithing.AnvilObjects,
            "BarbarianCourseObjects" to net.dodian.uber.game.content.objects.impl.agility.BarbarianCourseObjects,
            "BankBoothObjects" to net.dodian.uber.game.content.objects.impl.banking.BankBoothObjects,
            "BankChestObjects" to net.dodian.uber.game.content.objects.impl.banking.BankChestObjects,
            "ChestObjects" to net.dodian.uber.game.content.objects.impl.thieving.ChestObjects,
            "CompostBinObjects" to net.dodian.uber.game.content.objects.impl.farming.CompostBinObjects,
            "DoorToggleObjects" to net.dodian.uber.game.content.objects.impl.doors.DoorToggleObjects,
            "FarmingPatchObjects" to net.dodian.uber.game.content.objects.impl.farming.FarmingPatchObjects,
            "FurnaceObjects" to net.dodian.uber.game.content.objects.impl.smithing.FurnaceObjects,
            "FarmingPatchGuideObjects" to net.dodian.uber.game.content.objects.impl.farming.FarmingPatchGuideObjects,
            "GemRocksObjects" to net.dodian.uber.game.content.objects.impl.mining.GemRocksObjects,
            "GnomeCourseObjects" to net.dodian.uber.game.content.objects.impl.agility.GnomeCourseObjects,
            "LadderObjects" to net.dodian.uber.game.content.objects.impl.travel.LadderObjects,
            "MiningRocksObjects" to net.dodian.uber.game.content.objects.impl.mining.MiningRocksObjects,
            "PassageObjects" to net.dodian.uber.game.content.objects.impl.travel.PassageObjects,
            "PartyRoomObjects" to net.dodian.uber.game.content.objects.impl.events.PartyRoomObjects,
            "PlunderObjects" to net.dodian.uber.game.content.objects.impl.thieving.PlunderObjects,
            "RangeObjects" to net.dodian.uber.game.content.objects.impl.cooking.RangeObjects,
            "ResourceFillingObjects" to net.dodian.uber.game.content.objects.impl.crafting.ResourceFillingObjects,
            "RunecraftingObjects" to net.dodian.uber.game.content.objects.impl.runecrafting.RunecraftingObjects,
            "SpecialMiningObjects" to net.dodian.uber.game.content.objects.impl.mining.SpecialMiningObjects,
            "SpinningWheelObjects" to net.dodian.uber.game.content.objects.impl.crafting.SpinningWheelObjects,
            "StaircaseObjects" to net.dodian.uber.game.content.objects.impl.travel.StaircaseObjects,
            "StallObjects" to net.dodian.uber.game.content.objects.impl.thieving.StallObjects,
            "TeleportObjects" to net.dodian.uber.game.content.objects.impl.travel.TeleportObjects,
            "VerticalTeleportObjects" to net.dodian.uber.game.content.objects.impl.travel.VerticalTeleportObjects,
            "WebObstacleObjects" to net.dodian.uber.game.content.objects.impl.travel.WebObstacleObjects,
            "WerewolfCourseObjects" to net.dodian.uber.game.content.objects.impl.agility.WerewolfCourseObjects,
            "WildernessCourseObjects" to net.dodian.uber.game.content.objects.impl.agility.WildernessCourseObjects,
            "WoodcuttingTreesObjects" to net.dodian.uber.game.content.objects.impl.woodcutting.WoodcuttingTreesObjects,
        )

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
