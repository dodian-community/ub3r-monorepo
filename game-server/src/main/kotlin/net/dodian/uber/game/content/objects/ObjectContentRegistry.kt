package net.dodian.uber.game.content.objects

import net.dodian.uber.game.model.Position
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object ObjectContentRegistry {
    private val logger = LoggerFactory.getLogger(ObjectContentRegistry::class.java)

    private data class RegisteredBinding(
        val moduleName: String,
        val content: ObjectContent,
        val binding: ObjectBinding,
    )

    private val loaded = AtomicBoolean(false)
    private val byObjectId = ConcurrentHashMap<Int, List<RegisteredBinding>>()
    private val definitions = mutableListOf<Pair<String, ObjectContent>>()

    private val resolutionComparator = compareByDescending<RegisteredBinding> { it.binding.matcher.specificity }
        .thenByDescending { it.binding.priority }
        .thenBy { it.moduleName }
        .thenBy { it.binding.matcher.describe() }

    fun ensureLoaded() {
        if (loaded.get()) return
        synchronized(this) {
            if (loaded.get()) return
            val pending = listOf(
                "AltarObjects" to net.dodian.uber.game.content.objects.impl.prayer.AltarObjects,
                "AnvilObjects" to net.dodian.uber.game.content.objects.impl.smithing.AnvilObjects,
                "BankBoothObjects" to net.dodian.uber.game.content.objects.impl.banking.BankBoothObjects,
                "BankChestObjects" to net.dodian.uber.game.content.objects.impl.banking.BankChestObjects,
                "ChestObjects" to net.dodian.uber.game.content.objects.impl.thieving.ChestObjects,
                "DoorToggleObjects" to net.dodian.uber.game.content.objects.impl.doors.DoorToggleObjects,
                "FurnaceObjects" to net.dodian.uber.game.content.objects.impl.smithing.FurnaceObjects,
                "FarmingPatchGuideObjects" to net.dodian.uber.game.content.objects.impl.farming.FarmingPatchGuideObjects,
                "GemRocksObjects" to net.dodian.uber.game.content.objects.impl.mining.GemRocksObjects,
                "GnomeCourseObjects" to net.dodian.uber.game.content.objects.impl.agility.GnomeCourseObjects,
                "LadderObjects" to net.dodian.uber.game.content.objects.impl.travel.LadderObjects,
                "LegacyResidualObjects" to net.dodian.uber.game.content.objects.impl.legacy.LegacyResidualObjects,
                "MiningRocksObjects" to net.dodian.uber.game.content.objects.impl.mining.MiningRocksObjects,
                "PassageObjects" to net.dodian.uber.game.content.objects.impl.travel.PassageObjects,
                "QuestSpecialObjects" to net.dodian.uber.game.content.objects.impl.quest.QuestSpecialObjects,
                "SpecialMiningObjects" to net.dodian.uber.game.content.objects.impl.mining.SpecialMiningObjects,
                "StaircaseObjects" to net.dodian.uber.game.content.objects.impl.travel.StaircaseObjects,
                "StallObjects" to net.dodian.uber.game.content.objects.impl.thieving.StallObjects,
                "TeleportObjects" to net.dodian.uber.game.content.objects.impl.travel.TeleportObjects,
                "WerewolfCourseObjects" to net.dodian.uber.game.content.objects.impl.agility.WerewolfCourseObjects,
                "WildernessCourseObjects" to net.dodian.uber.game.content.objects.impl.agility.WildernessCourseObjects,
            )
            pending
                .sortedBy { it.first }
                .forEach { (name, content) -> register(name, content) }
            loaded.set(true)
        }
    }

    fun register(content: ObjectContent) = register(content::class.simpleName ?: "ObjectContent", content)

    fun register(name: String, content: ObjectContent) {
        val bindings = content.bindings()
        val registeredBindings = bindings.map { RegisteredBinding(name, content, it) }
        validateInternalOverlaps(name, registeredBindings)

        for (entry in registeredBindings.sortedWith(compareBy({ it.binding.objectId }, { it.binding.matcher.describe() }))) {
            val existing = byObjectId[entry.binding.objectId].orEmpty()
            validateCrossModuleOverlap(entry, existing)
            byObjectId[entry.binding.objectId] = (existing + entry).sortedWith(resolutionComparator)
        }

        definitions += name to content
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
    }

    @JvmStatic
    fun resolve(objectId: Int, position: Position): ObjectContent? {
        ensureLoaded()
        return byObjectId[objectId]
            ?.firstOrNull { it.binding.matcher.matches(position) }
            ?.content
    }

    @JvmStatic
    fun resolveAll(objectId: Int, position: Position): List<ObjectContent> {
        ensureLoaded()
        val resolved = byObjectId[objectId]
            .orEmpty()
            .asSequence()
            .filter { it.binding.matcher.matches(position) }
            .map { it.content }
            .toList()
        return resolved.distinctBy { it::class.java.name }
    }

    @JvmStatic
    fun get(objectId: Int): ObjectContent? {
        ensureLoaded()
        return byObjectId[objectId]?.firstOrNull()?.content
    }

    internal fun bindingsForObjectForTests(objectId: Int): List<ObjectBinding> {
        return byObjectId[objectId].orEmpty().map { it.binding }
    }

    internal fun clearForTests() {
        loaded.set(true)
        byObjectId.clear()
        definitions.clear()
    }

    internal fun resetForTests() {
        loaded.set(false)
        byObjectId.clear()
        definitions.clear()
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
