package net.dodian.uber.game.engine.webapi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.cors.routing.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.config.webApiEnabled
import net.dodian.uber.game.engine.config.webApiPort
import net.dodian.uber.game.engine.config.webApiAllowedOrigins
import net.dodian.uber.game.engine.config.webApiOpsToken
import net.dodian.uber.game.engine.config.webApiTrustedProxyCidrs
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.persistence.db.dbConnection
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.UUID
import java.net.URI
import org.slf4j.LoggerFactory

data class ServerStatus(
    var playersOnline: Set<OnlinePlayer> = emptySet(),
    val launchedAt: LocalDateTime = LocalDateTime.now(),
)

data class OnlinePlayer(
    val username: String
)

private fun getOnlinePlayers() = PlayerRegistry.playersOnline.map { (_, player) ->
    OnlinePlayer(player.playerName)
}

private val operationPaths = setOf(
    "/ready",
    "/content/manifest",
    "/prometheus",
    "/metrics",
    "/metrics/dashboard",
    "/dashboard",
    "/api/metrics/history",
)

private fun isOperationRoute(path: String): Boolean =
    path in operationPaths || path.startsWith("/api/v1/operations/")

private fun isHiscoresRoute(path: String): Boolean =
    path.contains("hiscore", ignoreCase = true) ||
        path.contains("highscore", ignoreCase = true) ||
        path.endsWith("/index_lite.ws")

private suspend fun respondApiError(
    call: ApplicationCall,
    status: HttpStatusCode,
    code: String,
    requestId: String,
) {
    call.respondText(
        mapper.writeValueAsString(
            linkedMapOf(
                "error" to code,
                "status" to status.value,
                "requestId" to requestId,
            ),
        ),
        ContentType.Application.Json,
        status,
    )
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
    private val negativeHiscoresCache = ConcurrentHashMap<String, Long>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val refreshInProgress = AtomicBoolean(false)
    private val requestGuard = WebRequestGuard()

    private fun updateHiscoresCache() {
        if (!refreshInProgress.compareAndSet(false, true)) return
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
                appendSkill(-1, 1, 0) // Sailing (unranked)

                for (i in 25..109) {
                    csvBuilder.append("-1,-1\n")
                }

                newCache[p.name.lowercase().trim().replace(" ", "_")] = csvBuilder.toString()
            }

            hiscoresCache.clear()
            hiscoresCache.putAll(newCache)
            negativeHiscoresCache.clear()
        } catch (ex: Exception) {
            logger.error("Hiscores cache refresh failed; retaining the previous cache", ex)
        } finally {
            refreshInProgress.set(false)
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
                    allowHeader(HttpHeaders.ContentType)
                    allowHeader(HttpHeaders.Authorization)
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Options)
                    allowCredentials = false
                    webApiAllowedOrigins.forEach { configuredOrigin ->
                        try {
                            val origin = URI(configuredOrigin)
                            if (!origin.host.isNullOrBlank() && !origin.scheme.isNullOrBlank()) {
                                allowHost(
                                    if (origin.port > 0) "${origin.host}:${origin.port}" else origin.host,
                                    schemes = listOf(origin.scheme),
                                )
                            }
                        } catch (exception: Exception) {
                            logger.warn("Ignoring invalid web API CORS origin {}", configuredOrigin)
                        }
                    }
                }
                intercept(ApplicationCallPipeline.Plugins) {
                    val requestId = UUID.randomUUID().toString()
                    call.response.header("X-Request-Id", requestId)
                    val method = call.request.httpMethod
                    if (method != HttpMethod.Get && method != HttpMethod.Options) {
                        respondApiError(call, HttpStatusCode.MethodNotAllowed, "method_not_allowed", requestId)
                        finish()
                        return@intercept
                    }
                    val contentLength = call.request.header(HttpHeaders.ContentLength)?.toLongOrNull() ?: 0L
                    val headerBytes = call.request.headers.entries().sumOf { (name, values) ->
                        name.length + values.sumOf { it.length }
                    }
                    if (contentLength > 8 * 1024L || headerBytes > 16 * 1024) {
                        respondApiError(call, HttpStatusCode.PayloadTooLarge, "request_too_large", requestId)
                        finish()
                        return@intercept
                    }

                    val path = call.request.path()
                    val operationRoute = method != HttpMethod.Options && isOperationRoute(path)
                    val clientIp = WebClientAddress.resolve(call, webApiTrustedProxyCidrs)
                    val rateClass = when {
                        operationRoute -> WebRateClass.OPERATIONS
                        isHiscoresRoute(path) -> WebRateClass.HISCORES
                        else -> WebRateClass.PUBLIC
                    }
                    val permit = requestGuard.enter(clientIp, rateClass)
                    if (!permit.permitted) {
                        net.dodian.uber.game.engine.metrics.OperationalTelemetry.incrementCounter(
                            "web.request.rate_limited",
                            1L,
                        )
                        call.response.header(HttpHeaders.RetryAfter, permit.retryAfterSeconds.toString())
                        respondApiError(call, HttpStatusCode.TooManyRequests, "rate_limited", requestId)
                        finish()
                        return@intercept
                    }
                    try {
                        net.dodian.uber.game.engine.metrics.OperationalTelemetry.incrementCounter(
                            "web.request.allowed",
                            1L,
                        )
                        try {
                            withTimeout(5_000L) { proceed() }
                        } catch (_: TimeoutCancellationException) {
                            if (!call.response.isCommitted) {
                                respondApiError(call, HttpStatusCode.ServiceUnavailable, "request_timeout", requestId)
                            }
                        }
                    } finally {
                        permit.release()
                    }
                }
                routing {
                    get("/api/server-status") {
                        serverStatus.playersOnline = getOnlinePlayers().toSet()
                        call.respondText(mapper.writeValueAsString(serverStatus), ContentType.Application.Json)
                    }
                    get("/api/v1/server-status") {
                        serverStatus.playersOnline = getOnlinePlayers().toSet()
                        call.respondText(mapper.writeValueAsString(serverStatus), ContentType.Application.Json)
                    }

                    get("/health") {
                        val health = mapOf(
                            "status" to "UP",
                            "players" to PlayerRegistry.getPlayerCount(),
                            "uptimeMs" to (System.currentTimeMillis() - Server.serverStartup),
                        )
                        call.respondText(mapper.writeValueAsString(health), ContentType.Application.Json)
                    }
                    get("/api/v1/health") {
                        val health = mapOf(
                            "status" to "UP",
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
                        val contentFaults = net.dodian.uber.game.api.content.ContentFaultCircuitBreaker.snapshot()
                        val quarantinedBindings = contentFaults["disabledBindings"] as List<*>
                        val skillsHealthy = quarantinedBindings.isEmpty()
                        val ready = dbOk && loopFresh && pluginsFrozen && skillsHealthy
                        val readiness = linkedMapOf<String, Any>(
                            "status" to if (ready) "READY" else "DEGRADED",
                            "database" to if (dbOk) "UP" else "DOWN",
                            "gameLoop" to if (loopFresh) "FRESH" else "STALE",
                            "skills" to if (skillsHealthy) "HEALTHY" else "DEGRADED",
                            "pluginBootstrap" to net.dodian.uber.game.engine.lifecycle.EnginePluginBootstrap.currentPhase().name,
                            "players" to PlayerRegistry.getPlayerCount(),
                            "telemetry" to net.dodian.uber.game.engine.metrics.OperationalTelemetry.snapshot(),
                            "contentFaults" to contentFaults,
                            "skillBetaTelemetry" to net.dodian.uber.game.engine.systems.skills.SkillBetaTelemetry.snapshot(),
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

                    get("/prometheus") {
                        if (net.dodian.uber.game.engine.config.metricsEnabled && net.dodian.uber.game.engine.config.metricsPrometheusEndpoint) {
                            call.respondText(
                                net.dodian.uber.game.engine.metrics.PrometheusMetricsRegistry.getPrometheusTextFormat(),
                                ContentType.Text.Plain
                            )
                        } else {
                            call.respond(HttpStatusCode.NotFound, "Prometheus metrics endpoint disabled in Settings.toml")
                        }
                    }

                    get("/metrics") {
                        if (net.dodian.uber.game.engine.config.metricsEnabled && net.dodian.uber.game.engine.config.metricsPrometheusEndpoint) {
                            call.respondText(
                                net.dodian.uber.game.engine.metrics.PrometheusMetricsRegistry.getPrometheusTextFormat(),
                                ContentType.Text.Plain
                            )
                        } else {
                            call.respond(HttpStatusCode.NotFound, "Prometheus metrics endpoint disabled in Settings.toml")
                        }
                    }

                    get("/metrics/dashboard") {
                        if (net.dodian.uber.game.engine.config.metricsEnabled && net.dodian.uber.game.engine.config.metricsDashboardEnabled) {
                            call.respondText(
                                net.dodian.uber.game.engine.metrics.MetricsDashboardHtml.renderHtml(),
                                ContentType.Text.Html
                            )
                        } else {
                            call.respond(HttpStatusCode.NotFound, "Metrics dashboard disabled in Settings.toml")
                        }
                    }

                    get("/dashboard") {
                        if (net.dodian.uber.game.engine.config.metricsEnabled && net.dodian.uber.game.engine.config.metricsDashboardEnabled) {
                            call.respondText(
                                net.dodian.uber.game.engine.metrics.MetricsDashboardHtml.renderHtml(),
                                ContentType.Text.Html
                            )
                        } else {
                            call.respond(HttpStatusCode.NotFound, "Metrics dashboard disabled in Settings.toml")
                        }
                    }

                    get("/api/metrics/history") {
                        if (net.dodian.uber.game.engine.config.metricsEnabled) {
                            val history = net.dodian.uber.game.engine.metrics.PrometheusMetricsRegistry.getHistorySnapshot()
                            call.respondText(
                                mapper.writeValueAsString(history),
                                ContentType.Application.Json
                            )
                        } else {
                            call.respond(HttpStatusCode.NotFound, "Metrics disabled in Settings.toml")
                        }
                    }

                    val handleHiscores: suspend (ApplicationCall) -> Unit = handleHiscores@{ call ->
                            val playerName = call.request.queryParameters["player"]
                                ?: call.request.queryParameters["username"]
                                ?: call.request.queryParameters["name"]
                                ?: call.parameters["player"]
                            if (playerName == null || playerName.isBlank()) {
                                call.respond(HttpStatusCode.BadRequest, "Missing 'player' query parameter.")
                            } else {
                                val sanitizedPlayerName = playerName.lowercase().trim().replace(" ", "_").replace("%20", "_")
                                if (!Regex("[a-z0-9_]{1,12}").matches(sanitizedPlayerName)) {
                                    call.respond(HttpStatusCode.BadRequest, "Invalid player name.")
                                    return@handleHiscores
                                }
                                val now = System.currentTimeMillis()
                                negativeHiscoresCache.entries.removeIf { now - it.value >= 5 * 60_000L }
                                val csvData = hiscoresCache[sanitizedPlayerName]
                                val requesterIp = WebClientAddress.resolve(call, webApiTrustedProxyCidrs)
                                if (csvData == null) {
                                    if (negativeHiscoresCache.size < 10_000) {
                                        negativeHiscoresCache.putIfAbsent(sanitizedPlayerName, now)
                                    }
                                    logger.info("hiscore_lookup player='{}' status=404 Not Found ip={}", sanitizedPlayerName, requesterIp)
                                    call.respond(HttpStatusCode.NotFound, "Player not found.")
                                } else {
                                    logger.info("hiscore_lookup player='{}' status=200 OK ip={}", sanitizedPlayerName, requesterIp)
                                    call.respondText(csvData, ContentType.Text.Plain)
                                }
                            }
                        }

                    get("/api/hiscores") { handleHiscores(call) }
                    get("/api/highscores") { handleHiscores(call) }
                    get("/api/v1/hiscores") { handleHiscores(call) }
                    get("/api/v1/highscores") { handleHiscores(call) }
                    get("/hiscores") { handleHiscores(call) }
                    get("/highscores") { handleHiscores(call) }
                    get("/index_lite.ws") { handleHiscores(call) }
                    get("/m=hiscore_oldschool/index_lite.ws") { handleHiscores(call) }
                    get("/api/hiscores/{player}") { handleHiscores(call) }
                    get("/api/highscores/{player}") { handleHiscores(call) }
                    get("/api/v1/hiscores/{player}") { handleHiscores(call) }
                    get("/api/v1/highscores/{player}") { handleHiscores(call) }
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
