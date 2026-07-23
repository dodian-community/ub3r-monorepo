package net.dodian.uber.game.engine.config

import io.github.cdimascio.dotenv.dotenv
import java.math.BigInteger

private val dotenv = dotenv()

private fun requiredEnv(key: String): String =
    dotenv[key]
        ?: throw IllegalStateException("Missing required environment variable: $key")

private fun requiredNonBlankEnv(key: String): String =
    requiredEnv(key).takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("Missing required environment variable: $key")

// Secrets/Credentials in .env
val databaseHost = requiredNonBlankEnv("DATABASE_HOST")
val databasePort = dotenv["DATABASE_PORT"]?.toInt() ?: 3306
val databaseName = requiredNonBlankEnv("DATABASE_NAME")
val databaseTablePrefix = dotenv["DATABASE_TABLE_PREFIX"] ?: ""
val databaseUsername = requiredNonBlankEnv("DATABASE_USERNAME")
val databasePassword = dotenv["DATABASE_PASSWORD"] ?: ""
val databaseInitialize = dotenv["DATABASE_INITIALIZE"]?.toBoolean() ?: false

val discordToken = dotenv["DISCORD_TOKEN"] ?: ""
val discordChannelId = dotenv["DISCORD_CHANNEL_ID"] ?: ""
val discordGuildId = dotenv["DISCORD_GUILD_ID"] ?: ""
val discordStaffAlertChannelId = dotenv["DISCORD_STAFF_ALERT_CHANNEL_ID"] ?: ""

val clientVersion = dotenv["CLIENT_VERSION"]?.toInt() ?: 12
val gameClientCustomVersion = dotenv["CLIENT_CUSTOM_VERSION"] ?: "dodian_client"

val rsaModulus = run {
    val str = dotenv["RSA_MODULUS"] ?: throw IllegalStateException("Missing required environment variable: RSA_MODULUS")
    try { BigInteger(str) } catch (e: Exception) { throw IllegalStateException("RSA_MODULUS is not a valid BigInteger", e) }
}
val rsaExponent = run {
    val str = dotenv["RSA_EXPONENT"] ?: throw IllegalStateException("Missing required environment variable: RSA_EXPONENT")
    try { BigInteger(str) } catch (e: Exception) { throw IllegalStateException("RSA_EXPONENT is not a valid BigInteger", e) }
}

// Perform validation
val _rsaValidation = run {
    if (rsaModulus.bitLength() < 1024) {
        throw IllegalStateException("RSA key modulus is too small (must be at least 1024 bits)")
    }
    val m = BigInteger.valueOf(12345)
    val e = BigInteger.valueOf(65537)
    val testDecrypted = m.modPow(e, rsaModulus).modPow(rsaExponent, rsaModulus)
    if (testDecrypted != m) {
        throw IllegalStateException("RSA private exponent and modulus do not form a matching key pair with public exponent 65537")
    }
}

// Delegation getters for Settings.toml
val serverName get() = SettingsLoader.settings.server.name
val serverPort get() = SettingsLoader.settings.network.gamePort
val serverDebugMode get() = SettingsLoader.settings.server.debug
val serverEnv get() = SettingsLoader.settings.server.environment
val nettyLeakDetection get() = SettingsLoader.settings.network.leakDetection
val dodianLogLevel get() = SettingsLoader.settings.server.logLevel
val webApiEnabled get() = SettingsLoader.settings.network.webEnabled
val webApiPort get() = SettingsLoader.settings.network.webPort

val gameWorldId get() = System.getenv("WORLD_ID")?.toIntOrNull() ?: System.getenv("WORLD")?.toIntOrNull() ?: SettingsLoader.settings.world.worldId
val gameConnectionsPerIp get() = SettingsLoader.settings.network.connectionsPerIp

val databasePoolMinSize get() = SettingsLoader.settings.databasePool.minSize
val databasePoolMaxSize get() = SettingsLoader.settings.databasePool.maxSize
val databasePoolConnectionTimeout get() = SettingsLoader.settings.databasePool.connectionTimeout
val databasePoolIdleTimeout get() = SettingsLoader.settings.databasePool.idleTimeout
val databasePoolMaxLifetime get() = SettingsLoader.settings.databasePool.maxLifetime

val runtimePhaseWarnMs = 300L

val gameMultiplierGlobalXp get() = SettingsLoader.settings.world.globalXpMultiplier
val swiftFupPort get() = SettingsLoader.settings.network.swiftfupPort
val swiftFupConnectionsPerIp get() = SettingsLoader.settings.network.swiftfupConnectionsPerIp
val swiftFupReadTimeoutSeconds get() = SettingsLoader.settings.network.swiftfupReadTimeoutSeconds
val swiftFupMaxTrackedIps get() = SettingsLoader.settings.network.swiftfupMaxTrackedIps
