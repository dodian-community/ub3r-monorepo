package net.dodian.utilities

import io.github.cdimascio.dotenv.dotenv

private val dotenv = dotenv()
private fun requiredEnv(key: String): String = dotenv[key]
    ?: throw IllegalStateException("Missing required environment variable: $key")

// Server Settings
val serverName = dotenv["SERVER_NAME"] ?: "Dodian"
val serverPort = dotenv["SERVER_PORT"]?.toInt() ?: 43594
val serverDebugMode = dotenv["SERVER_DEBUG_MODE"]?.toBoolean() ?: false
val serverEnv = dotenv["SERVER_ENVIRONMENT"] ?: "prod"
val nettyLeakDetection = dotenv["NETTY_LEAK_DETECTION"] ?: "disabled"
val debugPacketQueueMetrics = dotenv["DEBUG_PACKET_QUEUE_METRICS"]?.toBoolean() ?: false

// Database Settings
val databaseHost = requiredEnv("DATABASE_HOST")
val databasePort = dotenv["DATABASE_PORT"]?.toInt() ?: 3306
val databaseName = requiredEnv("DATABASE_NAME")
val databaseTablePrefix = dotenv["DATABASE_TABLE_PREFIX"] ?: ""
val databaseUsername = requiredEnv("DATABASE_USERNAME")
val databasePassword = requiredEnv("DATABASE_PASSWORD")
val databaseInitialize = dotenv["DATABASE_INITIALIZE"]?.toBoolean() ?: false

// Game Settings - Various
val gameWorldId = dotenv["GAME_WORLD_ID"]?.toInt() ?: 1
val gameConnectionsPerIp = dotenv["GAME_CONNECTIONS_PER_IP"]?.toInt() ?: 2

// Game Settings - Client
val gameClientCustomVersion = dotenv["CLIENT_CUSTOM_VERSION"] ?: "dodian_client"

// Database Pool Settings
val databasePoolMinSize = dotenv["DATABASE_POOL_MIN_SIZE"]?.toInt() ?: 5
val databasePoolMaxSize = dotenv["DATABASE_POOL_MAX_SIZE"]?.toInt() ?: 20
val databasePoolConnectionTimeout = dotenv["DATABASE_POOL_CONNECTION_TIMEOUT"]?.toLong() ?: 30000L
val databasePoolIdleTimeout = dotenv["DATABASE_POOL_IDLE_TIMEOUT"]?.toLong() ?: 600000L
val databasePoolMaxLifetime = dotenv["DATABASE_POOL_MAX_LIFETIME"]?.toLong() ?: 1800000L

// Async Persistence / World SQL Settings
// These are optional; existing .env files continue working with these defaults.
val asyncPlayerSaveEnabled = dotenv["ASYNC_PLAYER_SAVE_ENABLED"]?.toBoolean() ?: true
val asyncWorldDbEnabled = dotenv["ASYNC_WORLD_DB_ENABLED"]?.toBoolean() ?: true
val databaseSaveWorkers = dotenv["DATABASE_SAVE_WORKERS"]?.toInt() ?: 2
val databaseSaveRetryBaseMs = dotenv["DATABASE_SAVE_RETRY_BASE_MS"]?.toLong() ?: 250L
val databaseSaveRetryMaxMs = dotenv["DATABASE_SAVE_RETRY_MAX_MS"]?.toLong() ?: 5000L
val databaseSaveBurstAttempts = dotenv["DATABASE_SAVE_BURST_ATTEMPTS"]?.toInt() ?: 8

// Game Settings - Multipliers
val gameMultiplierGlobalXp = dotenv["GAME_MULTIPLIER_GLOBAL_XP"]?.toInt() ?: 1
