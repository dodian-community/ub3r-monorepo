package net.dodian.uber.game.persistence.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import net.dodian.uber.game.engine.config.databaseHost
import net.dodian.uber.game.engine.config.databaseName
import net.dodian.uber.game.engine.config.databasePassword
import net.dodian.uber.game.engine.config.databasePoolConnectionTimeout
import net.dodian.uber.game.engine.config.databasePoolIdleTimeout
import net.dodian.uber.game.engine.config.databasePoolMaxLifetime
import net.dodian.uber.game.engine.config.databasePoolMaxSize
import net.dodian.uber.game.engine.config.databasePoolMinSize
import net.dodian.uber.game.engine.config.databasePort
import net.dodian.uber.game.engine.config.databaseTablePrefix
import net.dodian.uber.game.engine.config.databaseUsername
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val logger: Logger = LoggerFactory.getLogger("Database")
private val databaseJdbcUrl = buildDatabaseJdbcUrl(databaseHost, databasePort, databaseName)

private val dataSourceLazy = lazy { createDataSource() }
private val schedulerLazy = lazy { Executors.newScheduledThreadPool(1) }
private val dataSource: HikariDataSource
    get() = dataSourceLazy.value

private fun createDataSource(): HikariDataSource {
    logger.debug("Initializing MariaDB connection pool...")
    validateDatabaseConfig(databaseHost, databaseName, databaseUsername)
    val config = HikariConfig().apply {
        jdbcUrl = databaseJdbcUrl
        username = databaseUsername
        password = databasePassword
        driverClassName = "org.mariadb.jdbc.Driver"
        minimumIdle = databasePoolMinSize
        maximumPoolSize = databasePoolMaxSize
        connectionTimeout = databasePoolConnectionTimeout
        idleTimeout = databasePoolIdleTimeout
        maxLifetime = databasePoolMaxLifetime

        addDataSourceProperty("autoReconnect", "true")
        addDataSourceProperty("serverTimezone", "UTC")

        poolName = "DodianDB-Pool"

        connectionTestQuery = "SELECT 1"
        validationTimeout = 5000

        leakDetectionThreshold = 30000

        isRegisterMbeans = true

        connectionInitSql = "SELECT 1"
    }

    logger.debug("Database target: {}", sanitizedJdbcTarget(databaseHost, databasePort, databaseName))

    return HikariDataSource(config).also { hikariDataSource ->
        logger.info(
            "database_pool_ready target={} pool={} size={}..{} timeout_ms={} leak_detection_ms={}",
            sanitizedJdbcTarget(databaseHost, databasePort, databaseName),
            config.poolName,
            config.minimumIdle,
            config.maximumPoolSize,
            config.connectionTimeout,
            config.leakDetectionThreshold,
        )

        startPoolMonitoring(hikariDataSource)
    }
}

val dbConnection: Connection
    get() {
        return try {
            dataSource.connection.apply {
                autoCommit = true
            }
        } catch (e: SQLException) {
            logger.error("Failed to get connection from pool: ${e.message}", e)
            val poolStats = dataSource.hikariPoolMXBean
            logger.error("Pool stats - Active: ${poolStats.activeConnections}, Idle: ${poolStats.idleConnections}, Total: ${poolStats.totalConnections}, Waiting: ${poolStats.threadsAwaitingConnection}")
            throw e
        }
    }

val dbStatement: Statement
    get() {
        logger.debug("Creating new statement from pooled connection")
        return dbConnection.createStatement()
    }

private fun startPoolMonitoring(hikariDataSource: HikariDataSource) {
    val scheduler = schedulerLazy.value
    scheduler.scheduleAtFixedRate({
        try {
            val poolStats = hikariDataSource.hikariPoolMXBean

            val utilizationPercent = (poolStats.activeConnections.toDouble() / databasePoolMaxSize) * 100
            if (utilizationPercent > 80) {
                logger.warn("[Pool Monitor] High pool utilization: ${utilizationPercent.toInt()}% (${poolStats.activeConnections}/${databasePoolMaxSize})")
            }

            if (poolStats.threadsAwaitingConnection > 0) {
                logger.warn("[Pool Monitor] ${poolStats.threadsAwaitingConnection} threads waiting for connections!")
            }
        } catch (e: RuntimeException) {
            logger.error("Error during pool monitoring", e)
        }
    }, 30, 30, TimeUnit.SECONDS)

    logger.debug("Pool monitoring started; warnings are emitted on high utilization or waiting threads")
}

fun closeConnectionPool() {
    if (!dataSourceLazy.isInitialized()) {
        return
    }

    logger.info("Shutting down connection pool...")

    if (schedulerLazy.isInitialized()) {
        val scheduler = schedulerLazy.value
        scheduler.shutdown()
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
        }
    }

    val dataSource = dataSourceLazy.value
    val poolStats = dataSource.hikariPoolMXBean
    logger.info("Final pool stats - Active: ${poolStats.activeConnections}, Idle: ${poolStats.idleConnections}, Total: ${poolStats.totalConnections}")

    if (poolStats.activeConnections > 0) {
        logger.warn("${poolStats.activeConnections} connections still active during shutdown - possible connection leaks!")
    }

    dataSource.close()
    logger.info("Connection pool closed")
}


enum class DbTables(val table: String) {
    WEB_USERS_TABLE("user"),
    WEB_THREAD("thread"),

    GAME_PLAYER_SESSIONS("uber3_sessions"),

    GAME_LOGS("uber3_log"),

    GAME_LOGS_PLAYER("uber3_logs"),
    GAME_LOGS_PLAYER_TRADES("uber3_trades"),
    GAME_LOGS_PLAYER_DUELS("duel_log"),
    GAME_LOGS_ITEMS("uber3_item_log"),
    GAME_LOGS_SHOP_TRANSACTIONS("shop_transaction_logs"),
    GAME_CHAT_LOGS("uber3_chat_log"),
    GAME_LOGS_STAFF_COMMANDS("uber3_command_log"),

    GAME_CHARACTERS("characters"),
    GAME_CHARACTERS_STATS("character_stats"),
    GAME_CHARACTERS_STATS_PROGRESS("character_stats_progress"),

    GAME_DOOR_DEFINITIONS("uber3_doors"),
    GAME_NPC_DEFINITIONS("uber3_npcs"),

    GAME_REFUND_ITEMS("uber3_refunds"),

    GAME_NPC_SPAWNS("uber3_spawn"),
    GAME_NPC_DROPS("uber3_drops"),

    GAME_PETE_CO("pete_co"),
    GAME_WORLDS("worlds")
    ;

    override fun toString() = databaseTablePrefix + this.table
}
