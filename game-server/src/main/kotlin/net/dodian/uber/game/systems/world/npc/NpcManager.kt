package net.dodian.uber.game.systems.world.npc

import java.util.TreeMap
import net.dodian.uber.game.content.npcs.NpcSpawnDef
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.npc.NpcData
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.world.npc.NpcDataRepository
import net.dodian.uber.game.systems.content.npcs.NpcContentRegistry
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
        logger.info("Loading NPC spawns from content registry modules")
        val contentSpawns = NpcContentRegistry.allSpawns()
        ensureDefinitionsForSpawnNpcIds(contentSpawns)
        val loaded = loadContentSpawns(contentSpawns)
        logger.info("Loaded {} content NPC spawns from modules.", loaded)
    }

    private fun ensureDefinitionsForSpawnNpcIds(spawns: List<NpcSpawnDef>) {
        val missingIds = spawns.asSequence().map { it.npcId }.distinct().filter { getData(it) == null }.sorted().toList()
        if (missingIds.isEmpty()) {
            return
        }

        var inserted = 0
        val stillMissing = ArrayList<Int>()
        for (npcId in missingIds) {
            try {
                NpcDataRepository.insertDefaultDefinition(npcId)
                val loaded = NpcDataRepository.loadDefinitionById(npcId)
                if (loaded != null) {
                    data[npcId] = loaded
                    inserted++
                } else {
                    stillMissing += npcId
                }
            } catch (e: Exception) {
                logger.error("Failed to upsert missing NPC definition for spawn npcId={}", npcId, e)
                stillMissing += npcId
            }
        }

        logger.warn(
            "Auto-upserted {} missing NPC definitions for content spawns ({} requested).",
            inserted,
            missingIds.size,
        )
        if (stillMissing.isNotEmpty()) {
            logger.error("NPC definitions still missing after auto-upsert attempt: {}", stillMissing.joinToString(","))
        }
    }

    private fun loadContentSpawns(contentSpawns: List<NpcSpawnDef>): Int {
        val total = contentSpawns.size
        var loaded = 0
        var skipped = 0
        var skippedInactive = 0
        var skippedDuplicatePosition = 0
        var skippedMissingData = 0
        var failed = 0
        val missingDataByNpcId = TreeMap<Int, Int>()
        val missingSpawnSamplesByNpcId = HashMap<Int, MutableList<Position>>()
        val seen = HashSet<String>(total)

        for (spawn in contentSpawns) {
            try {
                if (!spawn.live) {
                    skipped++
                    skippedInactive++
                    continue
                }
                if (getData(spawn.npcId) == null) {
                    skipped++
                    skippedMissingData++
                    missingDataByNpcId.merge(spawn.npcId, 1, Int::plus)
                    missingSpawnSamplesByNpcId
                        .computeIfAbsent(spawn.npcId) { ArrayList(3) }
                        .takeIf { it.size < 3 }
                        ?.add(Position(spawn.x, spawn.y, spawn.z))
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
                    effectiveWalkRadius(spawn),
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
                formatMissingNpcCounts(missingDataByNpcId, missingSpawnSamplesByNpcId),
            )
        }

        logger.info(
            "Loaded {}/{} content NPC spawns (skipped {}, inactive {}, duplicate {}, missing-data {}, failed {}).",
            loaded,
            total,
            skipped,
            skippedInactive,
            skippedDuplicatePosition,
            skippedMissingData,
            failed,
        )
        return loaded
    }

    private fun effectiveWalkRadius(spawn: NpcSpawnDef): Int {
        if (spawn.walkRadius > 0) {
            return spawn.walkRadius
        }
        val radial = maxOf(kotlin.math.abs(spawn.rx), kotlin.math.abs(spawn.ry), kotlin.math.abs(spawn.rx2), kotlin.math.abs(spawn.ry2))
        return maxOf(0, radial)
    }

    private fun formatMissingNpcCounts(
        missingDataByNpcId: Map<Int, Int>,
        missingSpawnSamplesByNpcId: Map<Int, List<Position>>,
    ): String {
        val builder = StringBuilder()
        var first = true
        for ((npcId, count) in missingDataByNpcId) {
            if (!first) {
                builder.append(", ")
            }
            val moduleName = NpcContentRegistry.get(npcId)?.name ?: "unmapped"
            val positions = missingSpawnSamplesByNpcId[npcId]
                .orEmpty()
                .joinToString(" | ") { "(${it.x},${it.y},${it.z})" }
                .ifEmpty { "no-sample" }
            builder
                .append(npcId)
                .append("x")
                .append(count)
                .append("[module=")
                .append(moduleName)
                .append(", samples=")
                .append(positions)
                .append("]")
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
                val field = NpcDataRepository.parseDefinitionField(table)
                NpcDataRepository.updateDefinitionField(id, field, value)
                c.sendMessage("You updated '${field.column}' with value '$value'!")
                reloadAllData(c, id)
            } catch (e: Exception) {
                when {
                    e.message?.contains("Unknown column") == true ->
                        c.sendMessage("row name '$table' do not exist in the database!")
                    e.message?.contains("Incorrect integer") == true ->
                        c.sendMessage("row name '$table' need a int value!")
                    e is IllegalArgumentException ->
                        c.sendMessage(e.message ?: "Invalid npc config field.")
                    else -> logger.error("NPC config reload failed for id={} field={}", id, table, e)
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
            logger.error("Error loading NPC definitions", e)
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
            logger.error("Error loading NPC drops", e)
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

    companion object {
        private val logger = LoggerFactory.getLogger(NpcManager::class.java)
    }
}
