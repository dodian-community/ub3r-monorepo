package net.dodian.uber.game.engine.config

import io.github.cdimascio.dotenv.dotenv

private val dotenv = dotenv()
private fun requiredEnv(key: String): String =
    dotenv[key]
        ?: throw IllegalStateException("Missing required environment variable: $key")

private fun requiredNonBlankEnv(key: String): String =
    requiredEnv(key).takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("Missing required environment variable: $key")

// Server Settings
val serverName = dotenv["SERVER_NAME"] ?: "Dodian"
val serverPort = dotenv["SERVER_PORT"]?.toInt() ?: 43594
val serverDebugMode = dotenv["SERVER_DEBUG_MODE"]?.toBoolean() ?: false
val serverEnv = dotenv["SERVER_ENVIRONMENT"] ?: "prod"
val nettyLeakDetection = dotenv["NETTY_LEAK_DETECTION"] ?: "disabled"
val dodianLogLevel = dotenv["DODIAN_LOG_LEVEL"] ?: "info"
val webApiEnabled = dotenv["WEB_API_ENABLED"]?.toBoolean() ?: true
val webApiPort = dotenv["WEB_API_PORT"]?.toInt() ?: 8080

// Database Settings
val databaseHost = requiredNonBlankEnv("DATABASE_HOST")
val databasePort = dotenv["DATABASE_PORT"]?.toInt() ?: 3306
val databaseName = requiredNonBlankEnv("DATABASE_NAME")
val databaseTablePrefix = dotenv["DATABASE_TABLE_PREFIX"] ?: ""
val databaseUsername = requiredNonBlankEnv("DATABASE_USERNAME")
val databasePassword = dotenv["DATABASE_PASSWORD"] ?: ""
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

val runtimePhaseWarnMs = 300L

// Game Settings - Multipliers
val gameMultiplierGlobalXp = dotenv["GAME_MULTIPLIER_GLOBAL_XP"]?.toInt() ?: 1
