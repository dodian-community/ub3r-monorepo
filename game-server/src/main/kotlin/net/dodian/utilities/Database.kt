package net.dodian.utilities

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val logger: Logger = LogManager.getLogger("Database")
private val jdbcUrl = "jdbc:mysql://$databaseHost:$databasePort/$databaseName?serverTimezone=UTC&autoReconnect=true"

private val dataSourceLazy = lazy { createDataSource() }
private val schedulerLazy = lazy { Executors.newScheduledThreadPool(1) }
private val dataSource: HikariDataSource
    get() = dataSourceLazy.value

private fun createDataSource(): HikariDataSource {
    logger.info("Initializing MySQL connection pool...")
    val config = HikariConfig().apply {
        jdbcUrl = net.dodian.utilities.jdbcUrl
        username = databaseUsername
        password = databasePassword
        driverClassName = "com.mysql.cj.jdbc.Driver"

        // Pool configuration
        minimumIdle = databasePoolMinSize
        maximumPoolSize = databasePoolMaxSize
        connectionTimeout = databasePoolConnectionTimeout
        idleTimeout = databasePoolIdleTimeout
        maxLifetime = databasePoolMaxLifetime

        // Connection properties
        addDataSourceProperty("autoReconnect", "true")
        addDataSourceProperty("serverTimezone", "UTC")

        // Pool name for monitoring
        poolName = "DodianDB-Pool"

        // Validation
        connectionTestQuery = "SELECT 1"
        validationTimeout = 5000

        // Leak detection (warn after 30 seconds)
        // This is a key feature. Hikari will log a warning with the stack trace
        // of where the connection was acquired if it's not closed within this time.
        leakDetectionThreshold = 30000

        // Enable JMX monitoring
        isRegisterMbeans = true

        // Connection init SQL for debugging
        connectionInitSql = "SELECT 1"
    }

    return HikariDataSource(config).also { hikariDataSource ->
        logger.info("Connection pool initialized:")
        logger.info("  - Pool name: ${config.poolName}")
        logger.info("  - Min connections: ${config.minimumIdle}")
        logger.info("  - Max connections: ${config.maximumPoolSize}")
        logger.info("  - Connection timeout: ${config.connectionTimeout}ms")
        logger.info("  - Leak detection: ${config.leakDetectionThreshold}ms (This will show where leaking connections are acquired)")
        logger.info("  - Database: ${config.jdbcUrl}")

        // Start pool monitoring
        startPoolMonitoring(hikariDataSource)
    }
}

val dbConnection: Connection
    get() {
        return try {
            val connection = dataSource.connection.apply {
                autoCommit = true
            }
            // Capture the stack trace at the moment the connection is requested.
            val acquisitionStackTrace = Thread.currentThread().stackTrace

            logger.debug("Connection obtained from pool (Active: ${(dataSource.hikariPoolMXBean.activeConnections)}/${(dataSource.hikariPoolMXBean.totalConnections)})")

            // Return a proxy to intercept method calls, like 'close()'.
            Proxy.newProxyInstance(
                Connection::class.java.classLoader,
                arrayOf(Connection::class.java),
                ConnectionInvocationHandler(connection, acquisitionStackTrace)
            ) as Connection

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
            logger.info("[Pool Monitor] Active: ${poolStats.activeConnections}, Idle: ${poolStats.idleConnections}, Total: ${poolStats.totalConnections}, Waiting: ${poolStats.threadsAwaitingConnection}")

            // Warn if pool utilization is high
            val utilizationPercent = (poolStats.activeConnections.toDouble() / databasePoolMaxSize) * 100
            if (utilizationPercent > 80) {
                logger.warn("[Pool Monitor] High pool utilization: ${utilizationPercent.toInt()}% (${poolStats.activeConnections}/${databasePoolMaxSize})")
            }

            // Warn if threads are waiting
            if (poolStats.threadsAwaitingConnection > 0) {
                logger.warn("[Pool Monitor] ${poolStats.threadsAwaitingConnection} threads waiting for connections!")
            }
        } catch (e: Exception) {
            logger.error("Error during pool monitoring", e)
        }
    }, 30, 30, TimeUnit.SECONDS)

    logger.info("Pool monitoring started - logging stats every 30 seconds")
}

fun closeConnectionPool() {
    if (!dataSourceLazy.isInitialized()) {
        return
    }

    logger.info("Shutting down connection pool...")

    // Stop monitoring
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

    // Close pool
    val dataSource = dataSourceLazy.value
    val poolStats = dataSource.hikariPoolMXBean
    logger.info("Final pool stats - Active: ${poolStats.activeConnections}, Idle: ${poolStats.idleConnections}, Total: ${poolStats.totalConnections}")

    if (poolStats.activeConnections > 0) {
        logger.warn("${poolStats.activeConnections} connections still active during shutdown - possible connection leaks!")
    }

    dataSource.close()
    logger.info("Connection pool closed")
}

/**
 * A dynamic proxy handler that wraps a JDBC Connection. It intercepts the 'close' method
 * to add enhanced logging, but delegates all other calls to the original connection.
 *
 * @param connection The real JDBC Connection object.
 * @param acquisitionStackTrace The stack trace captured when this connection was acquired from the pool.
 */
private class ConnectionInvocationHandler(
    private val connection: Connection,
    private val acquisitionStackTrace: Array<StackTraceElement>
) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        // Intercept the 'close()' method call.
        if (method.name == "close") {
            // Log where the connection was acquired from, useful for debugging.
            // Use a helper function to format the stack trace for readability.
            logger.debug("Connection closed. Acquired at:\n${formatStackTrace(acquisitionStackTrace)}")
        }

        // Delegate the actual method call to the original connection object.
        return try {
            // The 'args ?: emptyArray()' handles methods with no arguments.
            method.invoke(connection, *(args ?: emptyArray()))
        } catch (e: Exception) {
            // Log any exceptions that occur during method invocation on the connection.
            logger.error("Exception in connection proxy method '${method.name}': ${e.message}", e)
            throw e
        }
    }

    /**
     * Formats the stack trace for clean logging, skipping the initial irrelevant frames.
     */
    private fun formatStackTrace(stackTrace: Array<StackTraceElement>): String {
        // Skips the first few elements of the stack trace which are internal to the proxy and this class.
        return stackTrace
            .drop(3) // Adjust this number as needed to get to the calling code.
            .joinToString("\n\t") { "at $it" }
    }
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
    GAME_CHAT_LOGS("uber3_chat_log"),
    GAME_LOGS_STAFF_COMMANDS("uber3_command_log"),

    GAME_CHARACTERS("characters"),
    GAME_CHARACTERS_STATS("character_stats"),
    GAME_CHARACTERS_STATS_PROGRESS("character_stats_progress"),

    GAME_DOOR_DEFINITIONS("uber3_doors"),
    GAME_ITEM_DEFINITIONS("uber3_items"),
    GAME_NPC_DEFINITIONS("uber3_npcs"),
    GAME_OBJECT_DEFINITIONS("uber3_objects"),

    GAME_REFUND_ITEMS("uber3_refunds"),

    GAME_NPC_SPAWNS("uber3_spawn"),
    GAME_NPC_DROPS("uber3_drops"),

    GAME_PETE_CO("pete_co"),
    GAME_WORLDS("worlds")
    ;

    override fun toString() = databaseTablePrefix + this.table
}
