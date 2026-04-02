package net.dodian.uber.game.systems.content.npcs

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.dodian.uber.game.content.npcs.MYSQL_DEFAULT_STAT
import net.dodian.uber.game.content.npcs.NpcSpawnDef
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object NpcJsonSpawnOverlayLoader {
    private val logger = LoggerFactory.getLogger(NpcJsonSpawnOverlayLoader::class.java)
    private val mapper = jacksonObjectMapper()

    @Volatile
    private var cached: List<NpcSpawnDef>? = null

    @JvmStatic
    fun loadOverlaySpawns(): List<NpcSpawnDef> {
        if (!isEnabled()) {
            return emptyList()
        }
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val loaded = loadFromDirectory(resolveDirectory())
            cached = loaded
            return loaded
        }
    }

    @JvmStatic
    fun clearCacheForTests() {
        cached = null
    }

    private fun isEnabled(): Boolean {
        val property = System.getProperty("npc.spawns.json.enabled")
        if (!property.isNullOrBlank()) {
            return property.equals("true", ignoreCase = true)
        }
        val env = System.getenv("NPC_SPAWNS_JSON_ENABLED")
        return env.equals("true", ignoreCase = true)
    }

    private fun resolveDirectory(): Path {
        val configured = System.getProperty("npc.spawns.json.dir")?.trim().orEmpty()
        if (configured.isNotEmpty()) {
            return Paths.get(configured)
        }
        return Paths.get(
            "game-server/src/main/kotlin/net/dodian/uber/game/content/npcs/spawns/json",
        )
    }

    private fun loadFromDirectory(baseDir: Path): List<NpcSpawnDef> {
        if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
            logger.warn("NPC JSON spawn overlay enabled but directory was not found: {}", baseDir.toAbsolutePath())
            return emptyList()
        }
        val files =
            Files.walk(baseDir).use { stream ->
                stream
                    .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".json", ignoreCase = true) }
                    .sorted()
                    .toList()
            }
        if (files.isEmpty()) {
            logger.warn("NPC JSON spawn overlay enabled but no .json files were found in {}", baseDir.toAbsolutePath())
            return emptyList()
        }

        val overlays = ArrayList<NpcSpawnDef>()
        files.forEach { file ->
            try {
                val doc: NpcJsonSpawnDocument = mapper.readValue(file.toFile())
                doc.spawns.forEach { entry ->
                    overlays +=
                        NpcSpawnDef(
                            npcId = entry.npcId,
                            x = entry.x,
                            y = entry.y,
                            z = entry.z,
                            face = entry.face,
                            walkRadius = entry.walkRadius,
                            attackRange = entry.attackRange,
                            alwaysActive = entry.alwaysActive,
                            respawnTicks = entry.respawnTicks,
                            attack = entry.attack,
                            defence = entry.defence,
                            strength = entry.strength,
                            hitpoints = entry.hitpoints,
                            ranged = entry.ranged,
                            magic = entry.magic,
                        )
                }
            } catch (e: Exception) {
                logger.error("Failed to parse NPC spawn overlay file {}", file.toAbsolutePath(), e)
            }
        }
        logger.info("Loaded {} NPC JSON spawn overlay entries from {}", overlays.size, baseDir.toAbsolutePath())
        return overlays
    }
}

data class NpcJsonSpawnDocument(
    val location: String? = null,
    val spawns: List<NpcJsonSpawnEntry> = emptyList(),
)

data class NpcJsonSpawnEntry(
    val npcId: Int,
    val x: Int,
    val y: Int,
    val z: Int = 0,
    val face: Int = 0,
    val walkRadius: Int = 0,
    val attackRange: Int = 6,
    val alwaysActive: Boolean = false,
    val respawnTicks: Int = MYSQL_DEFAULT_STAT,
    val attack: Int = MYSQL_DEFAULT_STAT,
    val defence: Int = MYSQL_DEFAULT_STAT,
    val strength: Int = MYSQL_DEFAULT_STAT,
    val hitpoints: Int = MYSQL_DEFAULT_STAT,
    val ranged: Int = MYSQL_DEFAULT_STAT,
    val magic: Int = MYSQL_DEFAULT_STAT,
)
