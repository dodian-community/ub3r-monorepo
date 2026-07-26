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

val clientVersion = dotenv["CLIENT_VERSION"]?.toInt() ?: 12
val gameClientCustomVersion = dotenv["CLIENT_CUSTOM_VERSION"] ?: "dodian_client"

// Environment and world identity. These change per-deployment (not secrets), but they're read
// from .env rather than the git-tracked Settings.toml so spinning up another world or environment
// is a deploy-time env var, not a commit.
val serverEnv = (dotenv["SERVER_ENV"] ?: "dev").lowercase()
val gameWorldId = dotenv["WORLD_ID"]?.toIntOrNull() ?: 1

// Discord OAuth (gates new account creation - see AccountLoginService/DiscordApiService). Secrets,
// so these live in .env (gitignored), not Settings.toml (git-tracked).
val discordApiEnabled = dotenv["DISCORD_ENABLED"]?.toBoolean() ?: false
val discordClientId = dotenv["DISCORD_CLIENT_ID"] ?: ""
val discordClientSecret = dotenv["DISCORD_CLIENT_SECRET"] ?: ""
val discordRedirectUrl = dotenv["DISCORD_REDIRECT_URL"] ?: "http://localhost:8080/discord/callback"
val discordMaxAccountsPerDiscord = dotenv["DISCORD_MAX_ACCOUNTS_PER_DISCORD"]?.toIntOrNull() ?: 3

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
val serverPort get() = SettingsLoader.settings.network.gamePort
val serverDebugMode get() = if (SettingsLoader.isLoaded) SettingsLoader.settings.server.debug else true
val nettyLeakDetection get() = SettingsLoader.settings.network.leakDetection
val dodianLogLevel get() = SettingsLoader.settings.server.logLevel
val webApiEnabled get() = SettingsLoader.settings.network.webEnabled
val webApiPort get() = SettingsLoader.settings.network.webPort

val gameConnectionsPerIp get() = SettingsLoader.settings.network.connectionsPerIp

val databasePoolMinSize get() = SettingsLoader.settings.databasePool.minSize
val databasePoolMaxSize get() = SettingsLoader.settings.databasePool.maxSize
val databasePoolConnectionTimeout get() = SettingsLoader.settings.databasePool.connectionTimeout
val databasePoolIdleTimeout get() = SettingsLoader.settings.databasePool.idleTimeout
val databasePoolMaxLifetime get() = SettingsLoader.settings.databasePool.maxLifetime

val runtimePhaseWarnMs = 300L

val gameMultiplierGlobalXp get() = SettingsLoader.settings.world.globalXpMultiplier

// Falls back to 100 when settings haven't been loaded (e.g. unit tests constructing Player/Client
// directly without bootstrapping Server.main()) rather than throwing - this is read from Player's
// field initializers, a path exercised by nearly every test in the suite.
val gameMaxPlayers get() = if (SettingsLoader.isLoaded) SettingsLoader.settings.world.maxPlayers else 100

val rankBannedGroupId get() = if (SettingsLoader.isLoaded) SettingsLoader.settings.ranks.bannedGroupId else 3
val rankModGroupIds get() = if (SettingsLoader.isLoaded) SettingsLoader.settings.ranks.modGroupIds else listOf(5, 9)
val rankAdminGroupIds get() = if (SettingsLoader.isLoaded) SettingsLoader.settings.ranks.adminGroupIds else listOf(6, 10, 18, 35)
val rankDebugBypassGroupIds get() = if (SettingsLoader.isLoaded) SettingsLoader.settings.ranks.debugBypassGroupIds else listOf(11, 34, 40)

val metricsEnabled get() = if (SettingsLoader.isLoaded) SettingsLoader.settings.metrics.enabled else true
val metricsPrometheusEndpoint get() = if (SettingsLoader.isLoaded) SettingsLoader.settings.metrics.prometheusEndpoint else true
val metricsDashboardEnabled get() = if (SettingsLoader.isLoaded) SettingsLoader.settings.metrics.dashboardEnabled else true


