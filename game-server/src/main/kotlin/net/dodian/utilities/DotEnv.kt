package net.dodian.utilities

import io.github.cdimascio.dotenv.dotenv

private val dotenv = dotenv()

// Server Settings
val serverName = dotenv["SERVER_NAME"] ?: "Dodian"
val serverPort = dotenv["SERVER_PORT"]?.toInt() ?: 43594
val serverDebugMode = dotenv["SERVER_DEBUG_MODE"]?.toBoolean() ?: true
val serverEnv = dotenv["SERVER_ENVIRONMENT"] ?: "prod"

// Database Settings
val databaseHost = dotenv["DATABASE_HOST"] ?: "dodian.net"
val databasePort = dotenv["DATABASE_PORT"]?.toInt() ?: 3306
val databaseName = dotenv["DATABASE_NAME"] ?: "dodiannet"
val databaseTablePrefix = dotenv["DATABASE_TABLE_PREFIX"] ?: ""
val databaseUsername = dotenv["DATABASE_USERNAME"] ?: "moo"
val databasePassword = dotenv["DATABASE_PASSWORD"] ?: "hehe"
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

// Game Settings - Multipliers
val gameMultiplierGlobalXp = dotenv["GAME_MULTIPLIER_GLOBAL_XP"]?.toInt() ?: 1