package net.dodian.utilities

import io.github.cdimascio.dotenv.dotenv

private val dotenv = dotenv {
    ignoreIfMissing = true          // voorkomt crash als .env ontbreekt
    ignoreIfMalformed = true        // optioneel, maar handig
    systemProperties = true         // -DVAR=... (optioneel)
    // systemEnv = true            // als jouw versie dit ondersteunt; zie noot hieronder
}

// helper: eerst OS env, dan .env, dan default
private fun get(key: String): String? =
    System.getenv(key) ?: dotenv[key]

private fun getInt(key: String): Int? = get(key)?.toIntOrNull()
private fun getLong(key: String): Long? = get(key)?.toLongOrNull()
private fun getBool(key: String): Boolean? = get(key)?.lowercase()?.let { it == "true" || it == "1" || it == "yes" }

// Server Settings
val serverName = get("SERVER_NAME") ?: "Dodian"
val serverPort = getInt("SERVER_PORT") ?: 43894
val serverDebugMode = getBool("SERVER_DEBUG_MODE") ?: true
val serverEnv = get("SERVER_ENVIRONMENT") ?: "prod"

// Database Settings
val databaseHost = get("DATABASE_HOST") ?: "185.104.29.150"
val databasePort = getInt("DATABASE_PORT") ?: 3306
val databaseName = get("DATABASE_NAME") ?: "u43361p163932_dodian"
val databaseTablePrefix = get("DATABASE_TABLE_PREFIX") ?: ""
val databaseUsername = get("DATABASE_USERNAME") ?: "u43361p163932_dodian"
val databasePassword = get("DATABASE_PASSWORD") ?: "MLJTX4CN4VtJGaKD5tcz"
val databaseInitialize = getBool("DATABASE_INITIALIZE") ?: false

// Game Settings - Various
val gameWorldId = getInt("GAME_WORLD_ID") ?: 1
val gameConnectionsPerIp = getInt("GAME_CONNECTIONS_PER_IP") ?: 2

// Game Settings - Client
val gameClientCustomVersion = get("CLIENT_CUSTOM_VERSION") ?: "dodian_client"

// Database Pool Settings
val databasePoolMinSize = getInt("DATABASE_POOL_MIN_SIZE") ?: 5
val databasePoolMaxSize = getInt("DATABASE_POOL_MAX_SIZE") ?: 20
val databasePoolConnectionTimeout = getLong("DATABASE_POOL_CONNECTION_TIMEOUT") ?: 30000L
val databasePoolIdleTimeout = getLong("DATABASE_POOL_IDLE_TIMEOUT") ?: 600000L
val databasePoolMaxLifetime = getLong("DATABASE_POOL_MAX_LIFETIME") ?: 1800000L

// Game Settings - Multipliers
val gameMultiplierGlobalXp = getInt("GAME_MULTIPLIER_GLOBAL_XP") ?: 1
