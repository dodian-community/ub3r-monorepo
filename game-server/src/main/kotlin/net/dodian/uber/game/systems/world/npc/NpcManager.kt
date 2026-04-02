package net.dodian.uber.game.systems.world.npc

import java.util.TreeMap
import net.dodian.uber.game.systems.content.npcs.NpcContentRegistry
import net.dodian.uber.game.content.npcs.NpcSpawnDef
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.npc.NpcData
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.world.npc.NpcDataRepository
import org.slf4j.LoggerFactory

class NpcManager {
    val npcMap = HashMap<Int, Npc>()
    private val data = HashMap<Int, NpcData>()
    private var nextIndex = 1

    init {
        loadData()
    }

    fun getNpcs(): Collection<Npc> = npcMap.values

    fun getNpcData(): Collection<NpcData> = data.values

    fun loadSpawns() {
        logger.info("Loading NPC spawns from content registry only")
        val contentSpawns = NpcContentRegistry.allSpawns()
        validateRequiredContentSpawns(contentSpawns)
        val loaded = loadContentSpawns(contentSpawns)
        logger.info("Loaded {} content NPC spawns.", loaded)
    }

    private fun loadContentSpawns(contentSpawns: List<NpcSpawnDef>): Int {
        val total = contentSpawns.size
        var loaded = 0
        var skipped = 0
        var skippedDuplicatePosition = 0
        var skippedMissingData = 0
        var failed = 0
        val missingDataByNpcId = TreeMap<Int, Int>()
        val seen = HashSet<String>(total)

        for (spawn in contentSpawns) {
            try {
                if (getData(spawn.npcId) == null) {
                    skipped++
                    skippedMissingData++
                    missingDataByNpcId.merge(spawn.npcId, 1, Int::plus)
                    continue
                }
                val position = Position(spawn.x, spawn.y, spawn.z)
                val key = "${spawn.npcId}:${spawn.x}:${spawn.y}:${spawn.z}"
                if (!seen.add(key) || hasSpawnAt(spawn.npcId, position)) {
                    skipped++
                    skippedDuplicatePosition++
                    continue
                }
                val npc = createNpc(spawn.npcId, position, spawn.face)
                npc.applySpawnOverrides(
                    spawn.respawnTicks,
                    spawn.attack,
                    spawn.defence,
                    spawn.strength,
                    spawn.hitpoints,
                    spawn.ranged,
                    spawn.magic,
                )
                npc.applySpawnBehaviorOverrides(
                    spawn.walkRadius,
                    spawn.attackRange,
                    spawn.alwaysActive,
                    spawn.condition,
                )
                loaded++
            } catch (e: Exception) {
                failed++
                logger.error(
                    "Failed to create content NPC spawn (id={}, x={}, y={}, z={}).",
                    spawn.npcId,
                    spawn.x,
                    spawn.y,
                    spawn.z,
                    e,
                )
            }
        }

        if (missingDataByNpcId.isNotEmpty()) {
            logger.warn(
                "Skipped {} content NPC spawns with missing NPC definitions: {}",
                skippedMissingData,
                formatMissingNpcCounts(missingDataByNpcId),
            )
        }

        logger.info(
            "Loaded {}/{} content NPC spawns (skipped {}, duplicate {}, missing-data {}, failed {}).",
            loaded,
            total,
            skipped,
            skippedDuplicatePosition,
            skippedMissingData,
            failed,
        )
        return loaded
    }

    private fun validateRequiredContentSpawns(contentSpawns: List<NpcSpawnDef>) {
        val missingDefinitions = REQUIRED_CONTENT_NPC_DEFINITIONS.filter { getData(it) == null }.sorted()
        if (missingDefinitions.isNotEmpty()) {
            throw IllegalStateException("Missing required NPC definitions for content spawns: $missingDefinitions")
        }

        val available = HashSet<SpawnKey>(contentSpawns.size)
        for (spawn in contentSpawns) {
            available += SpawnKey(spawn.npcId, spawn.x, spawn.y, spawn.z)
        }
        val missingSpawns = REQUIRED_CONTENT_SPAWNS.filterNot { it in available }
        if (missingSpawns.isNotEmpty()) {
            throw IllegalStateException(
                "Missing required content NPC spawns: ${missingSpawns.joinToString { "${it.npcId}@(${it.x},${it.y},${it.z})" }}",
            )
        }
    }

    private fun formatMissingNpcCounts(missingDataByNpcId: Map<Int, Int>): String {
        val builder = StringBuilder()
        var first = true
        for ((npcId, count) in missingDataByNpcId) {
            if (!first) {
                builder.append(", ")
            }
            builder.append(npcId).append("x").append(count)
            first = false
        }
        return builder.toString()
    }

    private fun hasSpawnAt(npcId: Int, position: Position): Boolean {
        for (npc in npcMap.values) {
            if (npc.id != npcId) {
                continue
            }
            val npcPosition = npc.position
            if (npcPosition.x == position.x && npcPosition.y == position.y && npcPosition.z == position.z) {
                return true
            }
        }
        return false
    }

    fun findNpcByIdAtPosition(npcId: Int, x: Int, y: Int, z: Int): Npc? {
        for (npc in npcMap.values) {
            val position = npc.position
            if (npc.id == npcId && position.x == x && position.y == y && position.z == z) {
                return npc
            }
        }
        return null
    }

    fun reloadDrops(c: Client, id: Int) {
        try {
            val npcData = data[id]
            if (npcData == null) {
                c.sendMessage("No npc with id of $id")
                return
            }
            npcData.drops.clear()
            for (drop in NpcDataRepository.loadDropsForNpc(id)) {
                npcData.addDrop(drop.itemId, drop.amountMin, drop.amountMax, drop.percent, drop.rareShout)
            }
            c.sendMessage("Finished reloading all drops for ${npcData.name}")
        } catch (e: Exception) {
            println("npc drop wrong during drop reload..$e")
        }
    }

    fun reloadAllData(c: Client, id: Int) {
        try {
            val definition = NpcDataRepository.loadDefinitionById(id)
            if (definition != null) {
                data[id] = definition
                for (n in npcMap.values) {
                    if (n.id == id) {
                        n.reloadData()
                    }
                }
            }
            reloadDrops(c, id)
            c.sendMessage("Finished updating all '${getData(id)?.name}' npcs!")
        } catch (e: Exception) {
            println("npc drop wrong during reload of data..$e")
        }
    }

    fun reloadNpcConfig(c: Client, id: Int, table: String, value: String) {
        if (!data.containsKey(id)) {
            try {
                NpcDataRepository.insertDefaultDefinition(id)
                val defaultDefinition = NpcDataRepository.loadDefinitionById(id)
                if (defaultDefinition != null) {
                    data[id] = defaultDefinition
                    c.sendMessage("Added default config values to the npc!")
                }
            } catch (e: Exception) {
                println("error? $e")
            }
        } else if (!table.equals("new npc", ignoreCase = true)) {
            try {
                NpcDataRepository.updateDefinitionField(id, table, value)
                c.sendMessage("You updated '$table' with value '$value'!")
                reloadAllData(c, id)
            } catch (e: Exception) {
                when {
                    e.message?.contains("Unknown column") == true ->
                        c.sendMessage("row name '$table' do not exist in the database!")
                    e.message?.contains("Incorrect integer") == true ->
                        c.sendMessage("row name '$table' need a int value!")
                    else -> println("npc drop wrong during config reload..$e")
                }
            }
        }
    }

    fun loadData() {
        try {
            val definitions = NpcDataRepository.loadDefinitions()
            data.clear()
            data.putAll(definitions)
            logger.info("Loaded {} Npc Definitions", definitions.size)
        } catch (e: Exception) {
            println("Error loading NPC definitions: $e")
            e.printStackTrace()
        }

        try {
            val dropsByNpc = NpcDataRepository.loadAllDropsByNpcId()
            var amount = 0
            for ((id, drops) in dropsByNpc) {
                val definition = data[id]
                if (definition == null) {
                    logger.warn("Invalid NPC ID for drop: {}", id)
                    continue
                }
                for (drop in drops) {
                    amount++
                    definition.addDrop(drop.itemId, drop.amountMin, drop.amountMax, drop.percent, drop.rareShout)
                }
            }
            logger.info("Loaded {} Npc Drops", amount)
        } catch (e: Exception) {
            println("Error loading NPC drops: $e")
            e.printStackTrace()
        }
    }

    fun createNpc(id: Int, position: Position, face: Int): Npc {
        val npc = Npc(nextIndex, id, position, face)
        npcMap[nextIndex] = npc
        nextIndex++
        if (net.dodian.uber.game.Server.chunkManager != null) {
            npc.syncChunkMembership()
        }
        return npc
    }

    fun getNpc(index: Int): Npc = npcMap[index]
        ?: throw IllegalArgumentException("No NPC loaded at index $index")

    fun getName(id: Int): String = data[id]?.name ?: "NO NPC NAME YET!"

    fun getData(id: Int): NpcData? = data[id]

    private data class SpawnKey(
        val npcId: Int,
        val x: Int,
        val y: Int,
        val z: Int,
    )

    companion object {
        private val logger = LoggerFactory.getLogger(NpcManager::class.java)
        private val REQUIRED_CONTENT_NPC_DEFINITIONS = setOf(6080, 5924, 5926, 5927, 2267, 2265)
        private val REQUIRED_CONTENT_SPAWNS = setOf(
            SpawnKey(6080, 2475, 3428, 0),
            SpawnKey(6080, 2476, 3423, 1),
            SpawnKey(6080, 2475, 3419, 2),
            SpawnKey(6080, 2485, 3421, 2),
            SpawnKey(6080, 2487, 3423, 0),
            SpawnKey(6080, 2486, 3430, 0),
            SpawnKey(5924, 3540, 9873, 0),
            SpawnKey(5926, 3540, 9890, 0),
            SpawnKey(5926, 3537, 9903, 0),
            SpawnKey(5926, 3533, 9908, 0),
            SpawnKey(5926, 3528, 9913, 0),
            SpawnKey(5927, 3527, 9865, 0),
            SpawnKey(2267, 3248, 2794, 0),
            SpawnKey(2265, 3251, 2794, 0),
        )
    }
}
