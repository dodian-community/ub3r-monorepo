package net.dodian.uber.game.engine.config

import io.github.cdimascio.dotenv.dotenv
import java.math.BigInteger

private val dotenv = dotenv { ignoreIfMissing = true }

// Secrets/Credentials in .env. Host/name/username default to local-dev values (not secret) so the
// server, tests, and CI can boot without a .env file; production deployments override via real env vars.
val databaseHost = dotenv["DATABASE_HOST"]?.takeIf { it.isNotBlank() } ?: "127.0.0.1"
val databasePort = dotenv["DATABASE_PORT"]?.toInt() ?: 3306
val databaseName = dotenv["DATABASE_NAME"]?.takeIf { it.isNotBlank() } ?: "dodian"
val databaseTablePrefix = dotenv["DATABASE_TABLE_PREFIX"] ?: ""
val databaseUsername = dotenv["DATABASE_USERNAME"]?.takeIf { it.isNotBlank() } ?: "dodian"
val databasePassword = dotenv["DATABASE_PASSWORD"] ?: ""
val databaseInitialize = dotenv["DATABASE_INITIALIZE"]?.toBoolean() ?: false

val clientVersion = dotenv["CLIENT_VERSION"]?.toInt() ?: 13
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

// Dev-only fallback keypair (2048-bit, e=65537), used when RSA_MODULUS/RSA_EXPONENT aren't set via
// .env, so local dev/tests/CI can boot without secrets. Production MUST override both via real env
// vars - this pair is committed to source and provides no confidentiality once known.
private const val DEV_RSA_MODULUS =
    "28594733071843287045488567338617318348004242558695643607014244976462581434727681689118063908557530764184858205138910928513435720838915603591652363765452700687750993388272060857783518107626775161318983296812490913480536097505647242854937374747258632442583159190499024563608169286128122208003452582810496368569009812436199008910090849495125096786534836877531675705949014024143079456804421213835257847540118910504606003981893435856577019263108835673628836556453805192081661680311591808305114842049522068977044782884205132239991152780748709653752520563353554485402477393814465716946048367396715165507412340773954852321133"
private const val DEV_RSA_EXPONENT =
    "1203791271269902939690601603479640223417973133030826566851584629904638023992762466702423643494670604061614413048785499058067490940912280853706591263391427761379144464321568211950878533636606385249234400657730174257179899797337088103464794191490098217939285231343924939667591422561720694750774763507242618381692249870960074183478846090937476930162799894513945220996139858089877010504799052467220210954629334342165870782739374684121596705066559345419259032386962902042718709658080244836053906271679431878944972684137349198644512638787990149184202732149271007849710629392809845060512708440756245396922186616077075894209"

val rsaModulus = BigInteger(dotenv["RSA_MODULUS"] ?: DEV_RSA_MODULUS)
val rsaExponent = BigInteger(dotenv["RSA_EXPONENT"] ?: DEV_RSA_EXPONENT)

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
val webApiOpsToken = dotenv["WEB_API_OPS_TOKEN"] ?: ""
val webApiAllowedOrigins get() = if (SettingsLoader.isLoaded) SettingsLoader.settings.webSecurity.allowedOrigins else emptyList()
val webApiTrustedProxyCidrs get() = if (SettingsLoader.isLoaded) SettingsLoader.settings.webSecurity.trustedProxyCidrs else emptyList()

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

val discordRequireVerifiedEmail get() = if (SettingsLoader.isLoaded) SettingsLoader.settings.discord.requireVerifiedEmail else false
