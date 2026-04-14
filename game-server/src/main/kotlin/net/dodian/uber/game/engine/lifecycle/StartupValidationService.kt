package net.dodian.uber.game.engine.lifecycle

import net.dodian.uber.game.engine.config.databasePoolMaxSize
import net.dodian.uber.game.engine.config.databasePoolMinSize
import net.dodian.uber.game.engine.config.dodianLogLevel
import net.dodian.uber.game.engine.config.gameConnectionsPerIp
import net.dodian.uber.game.engine.config.gameWorldId
import net.dodian.uber.game.engine.config.serverPort
import net.dodian.uber.game.engine.config.webApiEnabled
import net.dodian.uber.game.engine.config.webApiPort
import net.dodian.uber.game.netty.NetworkConstants
import org.slf4j.LoggerFactory

object StartupValidationService {
    private val logger = LoggerFactory.getLogger(StartupValidationService::class.java)
    private val validLogLevels = setOf("trace", "debug", "info", "warn", "error")

    @JvmStatic
    fun validateOrThrow() {
        val violations = ArrayList<String>()

        if (serverPort !in 1..65535) {
            violations += "SERVER_PORT must be between 1 and 65535"
        }
        if (webApiEnabled && webApiPort !in 1..65535) {
            violations += "WEB_API_PORT must be between 1 and 65535 when WEB_API_ENABLED=true"
        }
        if (webApiEnabled && webApiPort == serverPort) {
            violations += "WEB_API_PORT must be different from SERVER_PORT"
        }
        if (gameWorldId <= 0) {
            violations += "GAME_WORLD_ID must be > 0"
        }
        if (gameConnectionsPerIp <= 0) {
            violations += "GAME_CONNECTIONS_PER_IP must be > 0"
        }
        if (databasePoolMinSize <= 0) {
            violations += "DATABASE_POOL_MIN_SIZE must be > 0"
        }
        if (databasePoolMaxSize < databasePoolMinSize) {
            violations += "DATABASE_POOL_MAX_SIZE must be >= DATABASE_POOL_MIN_SIZE"
        }
        if (NetworkConstants.PACKET_PROCESS_LIMIT_PER_TICK <= 0) {
            violations += "PACKET_PROCESS_LIMIT_PER_TICK must be > 0"
        }
        if (NetworkConstants.PACKET_RATE_LIMIT_PER_WINDOW <= 0) {
            violations += "PACKET_RATE_LIMIT_PER_WINDOW must be > 0"
        }
        if (NetworkConstants.PACKET_RATE_LIMIT_PER_WINDOW < NetworkConstants.PACKET_PROCESS_LIMIT_PER_TICK) {
            violations += "PACKET_RATE_LIMIT_PER_WINDOW must be >= PACKET_PROCESS_LIMIT_PER_TICK"
        }
        if (dodianLogLevel.lowercase() !in validLogLevels) {
            violations += "DODIAN_LOG_LEVEL must be one of ${validLogLevels.sorted().joinToString(",")}"
        }

        check(violations.isEmpty()) {
            "Startup validation failed:\n${violations.joinToString("\n")}"
        }
        logger.info(
            "Startup validation passed: worldId={} gamePort={} webApiEnabled={} webApiPort={} dbPool={}..{} packetLimits={}/{}",
            gameWorldId,
            serverPort,
            webApiEnabled,
            webApiPort,
            databasePoolMinSize,
            databasePoolMaxSize,
            NetworkConstants.PACKET_PROCESS_LIMIT_PER_TICK,
            NetworkConstants.PACKET_RATE_LIMIT_PER_WINDOW,
        )
    }
}
