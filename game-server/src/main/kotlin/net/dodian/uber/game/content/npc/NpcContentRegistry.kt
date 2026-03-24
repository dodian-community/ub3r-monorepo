package net.dodian.uber.game.content.npc

import net.dodian.uber.game.plugin.PluginModuleIndex
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

object NpcContentRegistry {
    private val logger = LoggerFactory.getLogger(NpcContentRegistry::class.java)

    private val bootstrapped = AtomicBoolean(false)
    @Volatile
    private var byNpcId: Array<NpcContentDefinition?> = emptyArray()
    private val definitions = mutableListOf<NpcContentDefinition>()

    @JvmStatic
    fun bootstrap() {
        if (bootstrapped.get()) return
        synchronized(this) {
            if (bootstrapped.get()) return
            PluginModuleIndex.npcContents
                .sortedBy { it.name }
                .forEach(::register)
            rebuildLookupLocked()
            bootstrapped.set(true)
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

        if (content.entries.isNotEmpty()) {
            val entryNpcIds = content.entries.asSequence().map { it.npcId }.distinct().toSet()
            val declaredNpcIds = content.npcIds.toSet()
            if (entryNpcIds.intersect(declaredNpcIds).isEmpty()) {
                logger.warn(
                    "NpcContent {} entries ({}) do not overlap handled npcIds ({}).",
                    content.name,
                    entryNpcIds.sorted().joinToString(","),
                    declaredNpcIds.sorted().joinToString(","),
                )
            }
        }

        if (content.ownsSpawnDefinitions && content.entries.isEmpty()) {
            logger.warn("NpcContent {} owns spawn definitions but has no entries.", content.name)
        }

        val existingNpcIds = definitions.asSequence().flatMap { it.npcIds.asSequence() }.toSet()
        val duplicateNpcIds = content.npcIds.filter { it in existingNpcIds }.distinct().sorted()
        require(duplicateNpcIds.isEmpty()) {
            val details = duplicateNpcIds.joinToString(",") { npcId ->
                val existing = definitions.firstOrNull { it.npcIds.contains(npcId) }
                "$npcId(existing=${existing?.name})"
            }
            "Duplicate NpcContent registration for ${content.name}: $details"
        }

        definitions += content

        logger.debug(
            "Registered NpcContent {} for npcIds={}",
            content.name,
            content.npcIds.joinToString(","),
        )
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

    private fun rebuildLookupLocked() {
        val maxNpcId = definitions.asSequence().flatMap { it.npcIds.asSequence() }.maxOrNull() ?: -1
        val rebuilt = arrayOfNulls<NpcContentDefinition>(maxNpcId + 1)
        for (definition in definitions) {
            for (npcId in definition.npcIds) {
                if (npcId < 0) {
                    continue
                }
                val existing = rebuilt[npcId]
                if (existing != null && existing !== definition) {
                    logger.error(
                        "Duplicate NpcContentDefinition for npcId={} (existing={}, new={})",
                        npcId,
                        existing.name,
                        definition.name,
                    )
                } else {
                    rebuilt[npcId] = definition
                }
            }
        }
        byNpcId = rebuilt
    }
}
