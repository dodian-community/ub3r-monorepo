package net.dodian.uber.game.engine.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

data class Settings(
    @JsonProperty("server") val server: ServerSettings,
    @JsonProperty("network") val network: NetworkSettings,
    @JsonProperty("world") val world: WorldSettings,
    @JsonProperty("features") val features: FeatureSettings,
    @JsonProperty("database_pool") val databasePool: DatabasePoolSettings,
    @JsonProperty("discord") val discord: DiscordSettings = DiscordSettings()
)

data class DiscordSettings(
    @JsonProperty("enabled") val enabled: Boolean = false,
    @JsonProperty("client_id") val clientId: String = "1529916683769090078",
    @JsonProperty("client_secret") val clientSecret: String = "VrY9E_f8G1UKTyc-Kk9kboJ4EywFns9-",
    @JsonProperty("redirect_url") val redirectUrl: String = "http://localhost:8080/discord/callback",
    @JsonProperty("max_accounts_per_discord") val maxAccountsPerDiscord: Int = 3
)

data class ServerSettings(
    @JsonProperty("name") val name: String,
    @JsonProperty("environment") val environment: String,
    @JsonProperty("debug") val debug: Boolean,
    @JsonProperty("tick_duration") val tickDuration: Int,
    @JsonProperty("log_level") val logLevel: String
)

data class NetworkSettings(
    @JsonProperty("game_port") val gamePort: Int,
    @JsonProperty("swiftfup_port") val swiftfupPort: Int,
    @JsonProperty("web_port") val webPort: Int,
    @JsonProperty("web_enabled") val webEnabled: Boolean,
    @JsonProperty("connections_per_ip") val connectionsPerIp: Int,
    @JsonProperty("connection_timeout") val connectionTimeout: Int,
    @JsonProperty("packet_limit") val packetLimit: Int,
    @JsonProperty("leak_detection") val leakDetection: String,
    @JsonProperty("swiftfup_connections_per_ip") val swiftfupConnectionsPerIp: Int = 8,
    @JsonProperty("swiftfup_read_timeout_seconds") val swiftfupReadTimeoutSeconds: Int = 30,
    @JsonProperty("swiftfup_max_tracked_ips") val swiftfupMaxTrackedIps: Int = 10_000
)

data class WorldSettings(
    @JsonProperty("world_id") val worldId: Int,
    @JsonProperty("max_players") val maxPlayers: Int,
    @JsonProperty("global_xp_multiplier") val globalXpMultiplier: Int
)

data class FeatureSettings(
    @JsonProperty("trading") val trading: Boolean,
    @JsonProperty("dueling") val dueling: Boolean,
    @JsonProperty("pvp") val pvp: Boolean,
    @JsonProperty("dropping") val dropping: Boolean,
    @JsonProperty("banking") val banking: Boolean,
    @JsonProperty("shopping") val shopping: Boolean,
    @JsonProperty("public_chat_yell") val publicChatYell: Boolean
)

data class DatabasePoolSettings(
    @JsonProperty("min_size") val minSize: Int,
    @JsonProperty("max_size") val maxSize: Int,
    @JsonProperty("connection_timeout") val connectionTimeout: Long,
    @JsonProperty("idle_timeout") val idleTimeout: Long,
    @JsonProperty("max_lifetime") val maxLifetime: Long
)

object SettingsLoader {
    lateinit var settings: Settings
        private set

    val isLoaded: Boolean
        get() = ::settings.isInitialized

    fun load() {
        val path = System.getenv("SETTINGS_PATH") ?: "Settings.toml"
        val file = File(path)
        if (!file.exists()) {
            throw IllegalStateException("Settings file not found at: ${file.absolutePath}")
        }
        val mapper = TomlMapper().registerKotlinModule()
        try {
            settings = mapper.readValue(file, Settings::class.java)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse settings file: ${file.absolutePath}", e)
        }

        validate()
    }

    private fun validate() {
        val environment = settings.server.environment.lowercase()
        if (environment !in setOf("dev", "test", "staging", "prod", "production")) {
            throw IllegalStateException("server.environment must be dev, test, staging, prod, or production")
        }
        if (environment in setOf("prod", "production") && settings.server.debug) {
            throw IllegalStateException("server.debug must be false in production")
        }
        // Validate ranges
        if (settings.network.gamePort !in 1..65535) {
            throw IllegalStateException("game_port must be in range 1-65535")
        }
        if (settings.network.swiftfupPort !in 1..65535) {
            throw IllegalStateException("swiftfup_port must be in range 1-65535")
        }
        if (settings.network.webPort !in 1..65535) {
            throw IllegalStateException("web_port must be in range 1-65535")
        }
        if (settings.network.connectionsPerIp < 1) {
            throw IllegalStateException("connections_per_ip must be at least 1")
        }
        if (settings.network.swiftfupConnectionsPerIp !in 1..64) {
            throw IllegalStateException("swiftfup_connections_per_ip must be in range 1-64")
        }
        if (settings.network.swiftfupReadTimeoutSeconds !in 5..300) {
            throw IllegalStateException("swiftfup_read_timeout_seconds must be in range 5-300")
        }
        if (settings.network.swiftfupMaxTrackedIps !in 100..100_000) {
            throw IllegalStateException("swiftfup_max_tracked_ips must be in range 100-100000")
        }
        if (settings.world.maxPlayers < 1) {
            throw IllegalStateException("max_players must be at least 1")
        }

        // Validate conflicting ports
        val ports = listOf(settings.network.gamePort, settings.network.swiftfupPort, settings.network.webPort)
        if (ports.distinct().size != ports.size) {
            throw IllegalStateException("Conflicting ports: game_port, swiftfup_port, and web_port must be unique! (game=${settings.network.gamePort}, swiftfup=${settings.network.swiftfupPort}, web=${settings.network.webPort})")
        }
    }
}
