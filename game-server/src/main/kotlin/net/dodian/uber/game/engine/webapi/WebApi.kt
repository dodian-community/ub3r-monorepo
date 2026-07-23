package net.dodian.uber.game.engine.webapi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.seconds
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.config.webApiEnabled
import net.dodian.uber.game.engine.config.webApiPort
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.persistence.db.dbConnection
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory

data class ServerStatus(
    var playersOnline: Set<OnlinePlayer> = emptySet(),
    val launchedAt: LocalDateTime = LocalDateTime.now(),
)

data class OnlinePlayer(
    val id: Int,
    val username: String
)

private fun getOnlinePlayers() = PlayerRegistry.playersOnline.map { (_, player) ->
    OnlinePlayer(player.dbId, player.playerName)
}

private val mapper: ObjectMapper = ObjectMapper()
    .findAndRegisterModules()
    .registerKotlinModule()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .enable(SerializationFeature.INDENT_OUTPUT)

data class PlayerStatRow(
    val name: String,
    val total: Int,
    val totalxp: Long,
    val attack: Long,
    val defence: Long,
    val strength: Long,
    val hitpoints: Long,
    val prayer: Long,
    val magic: Long,
    val ranged: Long,
    val cooking: Long,
    val woodcutting: Long,
    val fletching: Long,
    val fishing: Long,
    val firemaking: Long,
    val crafting: Long,
    val smithing: Long,
    val mining: Long,
    val herblore: Long,
    val agility: Long,
    val thieving: Long,
    val slayer: Long,
    val farming: Long,
    val runecrafting: Long
)

object WebApi {
    private val logger = LoggerFactory.getLogger(WebApi::class.java)
    private val started = AtomicBoolean(false)
    private val serverStatus = ServerStatus()
    private var serverEngine: NettyApplicationEngine? = null

    // Cache for highscores CSV responses by username (lowercase)
    private val hiscoresCache = ConcurrentHashMap<String, String>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    private fun updateHiscoresCache() {
        try {
            val statsList = mutableListOf<PlayerStatRow>()
            dbConnection.use { conn ->
                val query = """
                    SELECT c.name, s.total, s.totalxp, s.attack, s.defence, s.strength, s.hitpoints, s.prayer, s.magic, s.ranged, 
                           s.cooking, s.woodcutting, s.fletching, s.fishing, s.firemaking, s.crafting, s.smithing, s.mining, 
                           s.herblore, s.agility, s.thieving, s.slayer, s.farming, s.runecrafting 
                    FROM character_stats s
                    JOIN characters c ON c.id = s.uid
                """.trimIndent()
                conn.prepareStatement(query).use { stmt ->
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            val name = rs.getString("name") ?: continue
                            if (name.isBlank()) continue
                            statsList.add(PlayerStatRow(
                                name = name,
                                total = rs.getInt("total"),
                                totalxp = rs.getLong("totalxp"),
                                attack = rs.getLong("attack"),
                                defence = rs.getLong("defence"),
                                strength = rs.getLong("strength"),
                                hitpoints = rs.getLong("hitpoints"),
                                prayer = rs.getLong("prayer"),
                                magic = rs.getLong("magic"),
                                ranged = rs.getLong("ranged"),
                                cooking = rs.getLong("cooking"),
                                woodcutting = rs.getLong("woodcutting"),
                                fletching = rs.getLong("fletching"),
                                fishing = rs.getLong("fishing"),
                                firemaking = rs.getLong("firemaking"),
                                crafting = rs.getLong("crafting"),
                                smithing = rs.getLong("smithing"),
                                mining = rs.getLong("mining"),
                                herblore = rs.getLong("herblore"),
                                agility = rs.getLong("agility"),
                                thieving = rs.getLong("thieving"),
                                slayer = rs.getLong("slayer"),
                                farming = rs.getLong("farming"),
                                runecrafting = rs.getLong("runecrafting")
                            ))
                        }
                    }
                }
            }

            // Overall rank is determined by totalxp descending, then total descending
            val sortedOverall = statsList.sortedWith(compareByDescending<PlayerStatRow> { it.totalxp }.thenByDescending { it.total })
            val sortedAttack = statsList.sortedByDescending { it.attack }
            val sortedDefence = statsList.sortedByDescending { it.defence }
            val sortedStrength = statsList.sortedByDescending { it.strength }
            val sortedHitpoints = statsList.sortedByDescending { it.hitpoints }
            val sortedRanged = statsList.sortedByDescending { it.ranged }
            val sortedPrayer = statsList.sortedByDescending { it.prayer }
            val sortedMagic = statsList.sortedByDescending { it.magic }
            val sortedCooking = statsList.sortedByDescending { it.cooking }
            val sortedWoodcutting = statsList.sortedByDescending { it.woodcutting }
            val sortedFletching = statsList.sortedByDescending { it.fletching }
            val sortedFishing = statsList.sortedByDescending { it.fishing }
            val sortedFiremaking = statsList.sortedByDescending { it.firemaking }
            val sortedCrafting = statsList.sortedByDescending { it.crafting }
            val sortedSmithing = statsList.sortedByDescending { it.smithing }
            val sortedMining = statsList.sortedByDescending { it.mining }
            val sortedHerblore = statsList.sortedByDescending { it.herblore }
            val sortedAgility = statsList.sortedByDescending { it.agility }
            val sortedThieving = statsList.sortedByDescending { it.thieving }
            val sortedSlayer = statsList.sortedByDescending { it.slayer }
            val sortedFarming = statsList.sortedByDescending { it.farming }
            val sortedRunecrafting = statsList.sortedByDescending { it.runecrafting }

            val newCache = ConcurrentHashMap<String, String>()

            for (p in statsList) {
                val csvBuilder = StringBuilder()

                fun appendSkill(rank: Int, level: Int, xp: Long) {
                    csvBuilder.append(rank).append(",").append(level).append(",").append(xp).append("\n")
                }

                fun getRank(list: List<PlayerStatRow>): Int {
                    val idx = list.indexOf(p)
                    return if (idx == -1) -1 else idx + 1
                }

                appendSkill(getRank(sortedOverall), p.total, p.totalxp)
                appendSkill(getRank(sortedAttack), Skills.getLevelForExperience(p.attack.toInt()), p.attack)
                appendSkill(getRank(sortedDefence), Skills.getLevelForExperience(p.defence.toInt()), p.defence)
                appendSkill(getRank(sortedStrength), Skills.getLevelForExperience(p.strength.toInt()), p.strength)
                appendSkill(getRank(sortedHitpoints), Skills.getLevelForExperience(p.hitpoints.toInt()), p.hitpoints)
                appendSkill(getRank(sortedRanged), Skills.getLevelForExperience(p.ranged.toInt()), p.ranged)
                appendSkill(getRank(sortedPrayer), Skills.getLevelForExperience(p.prayer.toInt()), p.prayer)
                appendSkill(getRank(sortedMagic), Skills.getLevelForExperience(p.magic.toInt()), p.magic)
                appendSkill(getRank(sortedCooking), Skills.getLevelForExperience(p.cooking.toInt()), p.cooking)
                appendSkill(getRank(sortedWoodcutting), Skills.getLevelForExperience(p.woodcutting.toInt()), p.woodcutting)
                appendSkill(getRank(sortedFletching), Skills.getLevelForExperience(p.fletching.toInt()), p.fletching)
                appendSkill(getRank(sortedFishing), Skills.getLevelForExperience(p.fishing.toInt()), p.fishing)
                appendSkill(getRank(sortedFiremaking), Skills.getLevelForExperience(p.firemaking.toInt()), p.firemaking)
                appendSkill(getRank(sortedCrafting), Skills.getLevelForExperience(p.crafting.toInt()), p.crafting)
                appendSkill(getRank(sortedSmithing), Skills.getLevelForExperience(p.smithing.toInt()), p.smithing)
                appendSkill(getRank(sortedMining), Skills.getLevelForExperience(p.mining.toInt()), p.mining)
                appendSkill(getRank(sortedHerblore), Skills.getLevelForExperience(p.herblore.toInt()), p.herblore)
                appendSkill(getRank(sortedAgility), Skills.getLevelForExperience(p.agility.toInt()), p.agility)
                appendSkill(getRank(sortedThieving), Skills.getLevelForExperience(p.thieving.toInt()), p.thieving)
                appendSkill(getRank(sortedSlayer), Skills.getLevelForExperience(p.slayer.toInt()), p.slayer)
                appendSkill(getRank(sortedFarming), Skills.getLevelForExperience(p.farming.toInt()), p.farming)
                appendSkill(getRank(sortedRunecrafting), Skills.getLevelForExperience(p.runecrafting.toInt()), p.runecrafting)
                appendSkill(-1, 1, 0) // Hunter (unranked)
                appendSkill(-1, 1, 0) // Construction (unranked)

                for (i in 25..121) {
                    csvBuilder.append("-1,-1\n")
                }

                newCache[p.name.lowercase().trim().replace(" ", "_")] = csvBuilder.toString()
            }

            hiscoresCache.clear()
            hiscoresCache.putAll(newCache)
        } catch (ex: Exception) {
            logger.error("Hiscores cache refresh failed; retaining the previous cache", ex)
        }
    }

    @JvmStatic
    fun start() {
        if (!webApiEnabled) {
            return
        }
        if (!started.compareAndSet(false, true)) {
            return
        }
        try {
            scheduler.scheduleWithFixedDelay({
                updateHiscoresCache()
            }, 0, 5, TimeUnit.MINUTES)

            serverEngine = embeddedServer(Netty, port = webApiPort) {
                install(CORS) {
                    anyHost()
                    allowHeader(HttpHeaders.ContentType)
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Post)
                    allowMethod(HttpMethod.Put)
                    allowMethod(HttpMethod.Delete)
                    allowMethod(HttpMethod.Options)
                    allowCredentials = true
                }
                install(RateLimit) {
                    register {
                        rateLimiter(limit = 30, refillPeriod = 60.seconds)
                        requestKey { call -> call.request.local.remoteHost }
                    }
                }
                routing {
                    get("/api/server-status") {
                        serverStatus.playersOnline = getOnlinePlayers().toSet()
                        call.respondText(mapper.writeValueAsString(serverStatus), ContentType.Application.Json)
                    }

                    get("/health") {
                        val dbOk = try {
                            dbConnection.use { conn -> conn.isValid(2) }
                        } catch (_: Exception) {
                            false
                        }
                        val health = mapOf(
                            "status" to if (dbOk) "UP" else "DEGRADED",
                            "database" to if (dbOk) "UP" else "DOWN",
                            "players" to PlayerRegistry.getPlayerCount(),
                            "uptimeMs" to (System.currentTimeMillis() - Server.serverStartup),
                        )
                        call.respondText(mapper.writeValueAsString(health), ContentType.Application.Json)
                    }

                    get("/ready") {
                        val dbOk = try {
                            dbConnection.use { conn -> conn.isValid(2) }
                        } catch (_: Exception) {
                            false
                        }
                        val loopFresh = net.dodian.uber.game.engine.metrics.OperationalTelemetry.isGameLoopFresh()
                        val pluginsFrozen = net.dodian.uber.game.engine.lifecycle.EnginePluginBootstrap.isFrozen()
                        val contentPlatform = net.dodian.uber.game.api.plugin.ContentPlatformCatalog.snapshot()
                        val ready = dbOk && loopFresh && pluginsFrozen
                        val readiness = linkedMapOf<String, Any>(
                            "status" to if (ready) "READY" else "DEGRADED",
                            "database" to if (dbOk) "UP" else "DOWN",
                            "gameLoop" to if (loopFresh) "FRESH" else "STALE",
                            "pluginBootstrap" to net.dodian.uber.game.engine.lifecycle.EnginePluginBootstrap.currentPhase().name,
                            "players" to PlayerRegistry.getPlayerCount(),
                            "telemetry" to net.dodian.uber.game.engine.metrics.OperationalTelemetry.snapshot(),
                            "swiftFup" to mapOf("status" to "DISABLED"),
                            "contentFaults" to net.dodian.uber.game.api.content.ContentFaultCircuitBreaker.snapshot(),
                            "contentPlatform" to linkedMapOf(
                                "fingerprint" to contentPlatform.fingerprint,
                                "modules" to contentPlatform.modules.size,
                                "enabledModules" to contentPlatform.enabledCount,
                                "disabledModules" to contentPlatform.disabledCount,
                                "disabledModuleIds" to net.dodian.uber.game.api.plugin.ContentModuleFeatureState.disabledModuleIds(),
                            ),
                        )
                        call.respondText(
                            mapper.writeValueAsString(readiness),
                            ContentType.Application.Json,
                            if (ready) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
                        )
                    }

                    get("/content/manifest") {
                        val content = net.dodian.uber.game.api.plugin.ContentPlatformCatalog.snapshot()
                        call.respondText(
                            mapper.writeValueAsString(
                                linkedMapOf(
                                    "fingerprint" to content.fingerprint,
                                    "enabledModuleIds" to content.enabledModuleIds.sorted(),
                                    "modules" to content.modules,
                                    "routes" to net.dodian.uber.game.api.plugin.ContentRouteCatalog.snapshot(),
                                ),
                            ),
                            ContentType.Application.Json,
                        )
                    }

                    rateLimit {
                        val handleHiscores: suspend (ApplicationCall) -> Unit = { call ->
                            val playerName = call.request.queryParameters["player"]
                                ?: call.request.queryParameters["username"]
                                ?: call.request.queryParameters["name"]
                                ?: call.parameters["player"]
                            if (playerName == null || playerName.isBlank()) {
                                call.respond(HttpStatusCode.BadRequest, "Missing 'player' query parameter.")
                            } else {
                                val sanitizedPlayerName = playerName.lowercase().trim().replace(" ", "_").replace("%20", "_")
                                var csvData = hiscoresCache[sanitizedPlayerName]
                                if (csvData == null) {
                                    // Fallback: force refresh cache if cache missed
                                    updateHiscoresCache()
                                    csvData = hiscoresCache[sanitizedPlayerName]
                                }
                                if (csvData == null) {
                                    call.respond(HttpStatusCode.NotFound, "Player not found.")
                                } else {
                                    call.respondText(csvData, ContentType.Text.Plain)
                                }
                            }
                        }

                        get("/api/hiscores") { handleHiscores(call) }
                        get("/api/highscores") { handleHiscores(call) }
                        get("/hiscores") { handleHiscores(call) }
                        get("/highscores") { handleHiscores(call) }
                        get("/index_lite.ws") { handleHiscores(call) }
                        get("/m=hiscore_oldschool/index_lite.ws") { handleHiscores(call) }
                        get("/api/hiscores/{player}") { handleHiscores(call) }
                        get("/api/highscores/{player}") { handleHiscores(call) }
                    }
                }
            }.start(wait = false)
        } catch (exception: Exception) {
            started.set(false)
            throw exception
        }
    }

    @JvmStatic
    fun stop() {
        if (!started.compareAndSet(true, false)) {
            return
        }
        try {
            scheduler.shutdown()
            serverEngine?.stop(1000, 5000)
        } catch (_: Exception) {
        }
    }
}
