package net.dodian.uber.game.engine.systems.interaction.npcs

import net.dodian.uber.game.api.plugin.ContentModuleIndex
import net.dodian.uber.game.npc.NO_CLICK_HANDLER
import net.dodian.uber.game.npc.NpcContentDefinition
import net.dodian.uber.game.npc.NpcInteractionSource
import net.dodian.uber.game.npc.NpcSpawnDef
import net.dodian.uber.game.npc.hasInteractionHandlers
import net.dodian.uber.game.npc.optionLabel
import net.dodian.uber.game.api.plugin.ContentBootstrap
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

object NpcContentRegistry : ContentBootstrap {
    override val id: String = "npcs.registry"
    private val logger = LoggerFactory.getLogger(NpcContentRegistry::class.java)

    private val bootstrapped = AtomicBoolean(false)
    @Volatile
    private var byNpcId: Array<NpcContentDefinition?> = emptyArray()
    private val definitions = mutableListOf<NpcContentDefinition>()

    override fun bootstrap() {
        if (bootstrapped.get()) return
        synchronized(this) {
            if (bootstrapped.get()) return
            val pending = ContentModuleIndex.npcContents
            pending
                .sortedBy { it.name }
                .forEach(::register)
            rebuildLookupLocked()
            bootstrapped.set(true)
            val interactiveModules = definitions.count { it.hasInteractionHandlers() }
            val interactiveIds = definitions.filter { it.hasInteractionHandlers() }.sumOf { it.npcIds.size }
            logger.info(
                "Registered {} NPC content modules ({} interactive) covering {} interactive NPC ids.",
                definitions.size,
                interactiveModules,
                interactiveIds,
            )
            emitCapabilityReport()
            emitSpawnDiagnostics()
        }
    }

    fun ensureLoaded() {
        bootstrap()
    }

    fun register(content: NpcContentDefinition) {
        val localDuplicates = content.npcIds.groupBy { it }.filterValues { it.size > 1 }.keys
        require(localDuplicates.isEmpty()) {
            "Duplicate npcIds in ${content.name}: ${localDuplicates.sorted()}"
        }

        if (content.hasInteractionHandlers() && content.entries.isNotEmpty()) {
            val entryNpcIds = content.entries.asSequence().map { it.npcId }.distinct().toSet()
            val declaredNpcIds = content.npcIds.toSet()
            val undeclaredEntryNpcIds = (entryNpcIds - declaredNpcIds).sorted()
            require(undeclaredEntryNpcIds.isEmpty()) {
                "NpcContent ${content.name} has entries for undeclared npcIds: ${undeclaredEntryNpcIds.joinToString(",")}"
            }
        }
        require(!content.hasInteractionHandlers() || content.npcIds.isNotEmpty()) {
            "NpcContent ${content.name} has click handlers but no declared npcIds."
        }
        if (content.interactionSource != NpcInteractionSource.DSL) {
            error("NpcContent ${content.name} must use DSL option bindings (definition/plugin).")
        }

        if (content.ownsSpawnDefinitions && content.entries.isEmpty()) {
            logger.warn("NpcContent {} owns spawn definitions but has no entries.", content.name)
        }

        val existingNpcIds =
            definitions
                .asSequence()
                .filter { it.hasInteractionHandlers() }
                .flatMap { it.npcIds.asSequence() }
                .toSet()
        val candidateNpcIds = if (content.hasInteractionHandlers()) content.npcIds.asList() else emptyList()
        val duplicateNpcIds = candidateNpcIds.filter { it in existingNpcIds }.distinct().sorted()
        require(duplicateNpcIds.isEmpty()) {
            val details = duplicateNpcIds.joinToString(",") { npcId ->
                val existing = definitions.firstOrNull { it.npcIds.contains(npcId) }
                "$npcId(existing=${existing?.name})"
            }
            "Duplicate NpcContent registration for ${content.name}: $details"
        }

        definitions += content
    }

    @JvmStatic
    fun get(npcId: Int): NpcContentDefinition? {
        bootstrap()
        return byNpcId.getOrNull(npcId)
    }

    @JvmStatic
    fun allSpawns(): List<NpcSpawnDef> {
        bootstrap()
        return definitions.flatMap { it.entries }
    }

    @JvmStatic
    fun spawnSourceNpcIds(): Set<Int> {
        bootstrap()
        return definitions
            .asSequence()
            .filter { it.ownsSpawnDefinitions }
            .flatMap { it.npcIds.asSequence() }
            .toSet()
    }

    internal fun clearForTests() {
        bootstrapped.set(true)
        byNpcId = emptyArray()
        definitions.clear()
    }

    internal fun resetForTests() {
        bootstrapped.set(false)
        byNpcId = emptyArray()
        definitions.clear()
    }

    private fun emitCapabilityReport() {
        val rows = ArrayList<String>()
        definitions
            .asSequence()
            .filter { it.hasInteractionHandlers() }
            .forEach { def ->
                def.npcIds.sorted().forEach { npcId ->
                    val options = buildOptions(def)
                    rows += "npc=$npcId source=kotlin module=${def.name} options=$options"
                }
            }
        if (rows.isEmpty()) {
            logger.info("NPC capability report: no interactive entries loaded.")
            return
        }
        logger.info("NPC capability report entries={}", rows.size)
        rows.sorted().forEach { logger.info("NPC capability {}", it) }
    }

    private fun emitSpawnDiagnostics() {
        val banker = definitions.firstOrNull { it.name.equals("Banker", ignoreCase = true) } ?: return
        val bankerEntries = banker.entries.filter { it.npcId in setOf(394, 395, 7677) }
        val yanilleExpected = setOf(Pair(2615, 3094), Pair(2615, 3092), Pair(2615, 3091))
        val yanilleFound = bankerEntries.filter { (it.x to it.y) in yanilleExpected }
        logger.info(
            "NPC spawn diagnostic family=Banker totalEntries={} bankerEntries={} yanilleBankers={}",
            banker.entries.size,
            bankerEntries.size,
            yanilleFound.size,
        )
    }

    private fun buildOptions(def: NpcContentDefinition): String {
        val options = ArrayList<String>(5)
        if (def.onFirstClick !== NO_CLICK_HANDLER) options += "1:${def.optionLabel(1) ?: "first"}"
        if (def.onSecondClick !== NO_CLICK_HANDLER) options += "2:${def.optionLabel(2) ?: "second"}"
        if (def.onThirdClick !== NO_CLICK_HANDLER) options += "3:${def.optionLabel(3) ?: "third"}"
        if (def.onFourthClick !== NO_CLICK_HANDLER) options += "4:${def.optionLabel(4) ?: "fourth"}"
        if (def.onAttack !== NO_CLICK_HANDLER) options += "5:${def.optionLabel(5) ?: "attack"}"
        return options.joinToString("|")
    }

    private fun rebuildLookupLocked() {
        val maxNpcId = definitions.asSequence().flatMap { it.npcIds.asSequence() }.maxOrNull() ?: -1
        val rebuilt = arrayOfNulls<NpcContentDefinition>(maxNpcId + 1)
        for (definition in definitions) {
            for (npcId in definition.npcIds) {
                if (npcId < 0) continue
                val existing = rebuilt[npcId]
                if (existing == null || existing === definition) {
                    rebuilt[npcId] = definition
                    continue
                }

                val existingInteractive = existing.hasInteractionHandlers()
                val incomingInteractive = definition.hasInteractionHandlers()

                if (!existingInteractive && incomingInteractive) {
                    rebuilt[npcId] = definition
                    continue
                }

                if (existingInteractive != incomingInteractive) {
                    continue
                }

                error("Duplicate NpcContentDefinition for npcId=$npcId (existing=${existing.name}, new=${definition.name})")
            }
        }
        byNpcId = rebuilt
    }

}
