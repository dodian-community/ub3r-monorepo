package net.dodian.uber.game.content.npcs.spawns

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object NpcContentRegistry {
    private val logger = LoggerFactory.getLogger(NpcContentRegistry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byNpcId = ConcurrentHashMap<Int, NpcContentDefinition>()
    private val definitions = mutableListOf<NpcContentDefinition>()

    fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) return
        register(
            NpcContentDefinition(
                name = "Aubury",
                npcIds = Aubury.npcIds,
                ownsSpawnDefinitions = true,
                entries = Aubury.entries,
                onFirstClick = Aubury::onFirstClick,
                onSecondClick = Aubury::onSecondClick,
                onThirdClick = Aubury::onThirdClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Banker",
                npcIds = Banker.npcIds,
                entries = Banker.entries,
                onFirstClick = Banker::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Monk",
                npcIds = Monk.npcIds,
                entries = Monk.entries,
                onFirstClick = Monk::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "MakeoverMage",
                npcIds = MakeoverMage.npcIds,
                ownsSpawnDefinitions = true,
                entries = MakeoverMage.entries,
                onFirstClick = MakeoverMage::onFirstClick,
            ),
        )
    }

    fun register(content: NpcContentDefinition) {
        val localDuplicates = content.npcIds.groupBy { it }.filterValues { it.size > 1 }.keys
        require(localDuplicates.isEmpty()) {
            "Duplicate npcIds in ${content.name}: ${localDuplicates.sorted()}"
        }

        val duplicateNpcIds = content.npcIds.filter { byNpcId.containsKey(it) }.distinct().sorted()
        require(duplicateNpcIds.isEmpty()) {
            val details = duplicateNpcIds.joinToString(",") { npcId ->
                val existing = byNpcId[npcId]
                "$npcId(existing=${existing?.name})"
            }
            "Duplicate NpcContent registration for ${content.name}: $details"
        }

        definitions += content
        for (npcId in content.npcIds) {
            byNpcId[npcId] = content
        }

        logger.debug(
            "Registered NpcContent {} for npcIds={}",
            content.name,
            content.npcIds.joinToString(","),
        )
    }

    @JvmStatic
    fun get(npcId: Int): NpcContentDefinition? {
        ensureLoaded()
        return byNpcId[npcId]
    }

    @JvmStatic
    fun allSpawns(): List<NpcSpawnDef> {
        ensureLoaded()
        return definitions.flatMap { it.entries }
    }

    @JvmStatic
    fun spawnSourceNpcIds(): Set<Int> {
        ensureLoaded()
        return definitions
            .asSequence()
            .filter { it.ownsSpawnDefinitions }
            .flatMap { it.npcIds.asSequence() }
            .toSet()
    }

    internal fun clearForTests() {
        loaded.set(true)
        byNpcId.clear()
        definitions.clear()
    }

    internal fun resetForTests() {
        loaded.set(false)
        byNpcId.clear()
        definitions.clear()
    }
}
