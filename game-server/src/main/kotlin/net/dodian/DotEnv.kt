package net.dodian

import io.github.cdimascio.dotenv.dotenv

private val dotenv = dotenv()

// Server Settings
val serverName = dotenv["SERVER_NAME"] ?: "Dodian"
val serverPort = dotenv["SERVER_PORT"]?.toInt() ?: 43594
val serverDebugMode = dotenv["SERVER_DEBUG_MODE"]?.toBoolean() ?: false
val serverEnv = dotenv["SERVER_ENVIRONMENT"] ?: "prod"

// Database Settings
val databaseHost = dotenv["DATABASE_HOST"] ?: "localhost"
val databasePort = dotenv["DATABASE_PORT"]?.toInt() ?: 3306
val databaseName = dotenv["DATABASE_NAME"] ?: "dodiannet"
val databaseTablePrefix = dotenv["DATABASE_TABLE_PREFIX"] ?: ""
val databaseUsername = dotenv["DATABASE_USERNAME"] ?: "dodian_game"
val databasePassword = dotenv["DATABASE_PASSWORD"] ?: "abcd1234"

// Game Settings - Various
val gameWorldId = dotenv["GAME_WORLD_ID"]?.toInt() ?: 1
val gameConnectionsPerIp = dotenv["GAME_CONNECTIONS_PER_IP"]?.toInt() ?: 2

// Game Settings - Client
val gameClientCustomVersion = dotenv["GAME_CLIENT_CUSTOM_VERSION"] ?: "dodian_client"

// Game Settings - Multipliers
val gameMultiplierGlobalXp = dotenv["GAME_MULTIPLIER_GLOBAL_XP"]?.toInt() ?: 1