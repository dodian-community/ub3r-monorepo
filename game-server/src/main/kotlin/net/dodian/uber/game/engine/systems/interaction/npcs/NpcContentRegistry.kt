package net.dodian.uber.game.engine.systems.interaction.npcs

import net.dodian.uber.game.api.plugin.ContentModuleIndex
import net.dodian.uber.game.npc.NO_CLICK_HANDLER
import net.dodian.uber.game.npc.NpcContentDefinition
import net.dodian.uber.game.npc.NpcInteractionSource
import net.dodian.uber.game.npc.hasInteractionHandlers
import net.dodian.uber.game.npc.optionLabel
import net.dodian.uber.game.api.plugin.ContentBootstrap
import net.dodian.uber.game.model.entity.npc.Npc
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

object NpcContentRegistry : ContentBootstrap {
    override val id: String = "npcs.registry"
    private val logger = LoggerFactory.getLogger(NpcContentRegistry::class.java)

    private val bootstrapped = AtomicBoolean(false)
    @Volatile
    private var byNpcId: Array<NpcContentDefinition?> = emptyArray()
    @Volatile
    private var byProfileAndNpcId: Map<String, NpcContentDefinition> = emptyMap()
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

        require(!content.hasInteractionHandlers() || content.npcIds.isNotEmpty()) {
            "NpcContent ${content.name} has click handlers but no declared npcIds."
        }
        if (content.interactionSource != NpcInteractionSource.DSL) {
            error("NpcContent ${content.name} must use DSL option bindings (definition/plugin).")
        }

        val existingNpcIds =
            definitions
                .asSequence()
                .filter { it.hasInteractionHandlers() }
                .filter { it.profiles.isEmpty() && content.profiles.isEmpty() }
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
        rebuildLookupLocked()
    }

    @JvmStatic
    fun get(npcId: Int): NpcContentDefinition? {
        bootstrap()
        return byNpcId.getOrNull(npcId)
    }

    @JvmStatic
    fun get(npc: Npc): NpcContentDefinition? {
        bootstrap()
        return get(npc.id, npc.interactionProfile)
    }

    internal fun get(npcId: Int, profile: String?): NpcContentDefinition? {
        bootstrap()
        if (!profile.isNullOrBlank()) {
            byProfileAndNpcId[profileKey(profile, npcId)]?.let { return it }
        }
        return byNpcId.getOrNull(npcId)
    }

    @JvmStatic
    fun hasAttackHandler(npc: Npc): Boolean {
        bootstrap()
        val definition = get(npc) ?: return false
        return definition.onAttack !== NO_CLICK_HANDLER
    }

    @JvmStatic
    fun hasAttackHandler(npcId: Int): Boolean {
        bootstrap()
        val definition = byNpcId.getOrNull(npcId) ?: return false
        return definition.onAttack !== NO_CLICK_HANDLER
    }

    internal fun clearForTests() {
        bootstrapped.set(true)
        byNpcId = emptyArray()
        byProfileAndNpcId = emptyMap()
        definitions.clear()
    }

    internal fun resetForTests() {
        bootstrapped.set(false)
        byNpcId = emptyArray()
        byProfileAndNpcId = emptyMap()
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
                    val profile = def.profiles.sorted().joinToString("|").ifBlank { "global" }
                    rows += "npc=$npcId profile=$profile source=kotlin module=${def.name} options=$options"
                }
            }
        if (rows.isEmpty()) {
            logger.warn("NPC capability report: no interactive entries loaded.")
            return
        }
        // The "Registered N NPC content modules" line above already covers the counts.
        logger.debug("NPC capability report entries={}", rows.size)
        rows.sorted().forEach { logger.debug("NPC capability {}", it) }
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
        val profiled = HashMap<String, NpcContentDefinition>()
        for (definition in definitions) {
            for (npcId in definition.npcIds) {
                if (npcId < 0) continue
                if (definition.profiles.isNotEmpty()) {
                    for (profile in definition.profiles) {
                        val key = profileKey(profile, npcId)
                        val existing = profiled.putIfAbsent(key, definition)
                        require(existing == null || existing === definition) {
                            "Duplicate profiled NpcContentDefinition for npcId=$npcId profile=$profile " +
                                "(existing=${existing?.name}, new=${definition.name})"
                        }
                    }
                    continue
                }
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
        byProfileAndNpcId = profiled
    }

    private fun profileKey(profile: String, npcId: Int): String = "${NpcInteractionProfileRegistry.normalize(profile)}:$npcId"

}
