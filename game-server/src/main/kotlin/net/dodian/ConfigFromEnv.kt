package net.dodian

import net.dodian.models.config.DatabaseConfig
import net.dodian.models.config.ServerConfig

fun env(key: String): String? = System.getenv(key)

fun getConfigFromEnv() = ServerConfig().apply {
    name = env("SERVER_NAME") ?: name
    port = env("SERVER_PORT")?.toInt() ?: port
    clientSecret = env("CLIENT_SECRET") ?: clientSecret
    isDebugMode = env("DEBUG_MODE")?.toBoolean() ?: isDebugMode
    connectionsPerIp = env("MAX_CONNECTIONS_FROM_IP")?.toInt() ?: connectionsPerIp
    worldId = env("WORLD_ID")?.toInt() ?: worldId
    experienceMultiplier = env("EXPERIENCE_MULTIPLIER")?.toInt() ?: experienceMultiplier
    customClientVersion = env("CUSTOM_CLIENT_VERSION") ?: customClientVersion
    databaseConfig = DatabaseConfig().apply {
        host = env("DATABASE_HOST") ?: host
        port = env("DATABASE_PORT")?.toInt() ?: port
        database = env("DATABASE_NAME") ?: database
        tablePrefix = env("DATABASE_TABLE_PREFIX") ?: tablePrefix
        username = env("DATABASE_USERNAME") ?: username
        password = env("DATABASE_PASSWORD") ?: password
    }
}
