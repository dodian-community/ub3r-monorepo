package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.ContentModuleIndex
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
        val defaults = definitions.flatMap { it.entries }
        val overlays = NpcJsonSpawnOverlayLoader.loadOverlaySpawns()
        if (overlays.isEmpty()) {
            return defaults
        }
        val mergedByKey = LinkedHashMap<String, NpcSpawnDef>(defaults.size + overlays.size)
        defaults.forEach { mergedByKey[spawnKey(it)] = it }
        overlays.forEach { mergedByKey[spawnKey(it)] = it }
        return mergedByKey.values.toList()
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
                if (npcId < 0) continue
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

    private fun spawnKey(spawn: NpcSpawnDef): String {
        return "${spawn.npcId}:${spawn.x}:${spawn.y}:${spawn.z}"
    }
}
