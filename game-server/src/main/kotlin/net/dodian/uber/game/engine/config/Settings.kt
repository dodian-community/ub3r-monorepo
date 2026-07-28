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
    @JsonProperty("ranks") val ranks: RankSettings = RankSettings(),
    @JsonProperty("metrics") val metrics: MetricsSettings = MetricsSettings(),
    @JsonProperty("discord") val discord: DiscordSettings = DiscordSettings(),
    @JsonProperty("web_security") val webSecurity: WebSecuritySettings = WebSecuritySettings(),
)

data class WebSecuritySettings(
    @JsonProperty("allowed_origins") val allowedOrigins: List<String> = emptyList(),
    @JsonProperty("trusted_proxy_cidrs") val trustedProxyCidrs: List<String> = emptyList(),
)

data class DiscordSettings(
    @JsonProperty("require_verified_email") val requireVerifiedEmail: Boolean = false
)

data class MetricsSettings(
    @JsonProperty("enabled") val enabled: Boolean = true,
    @JsonProperty("prometheus_endpoint") val prometheusEndpoint: Boolean = true,
    @JsonProperty("dashboard_enabled") val dashboardEnabled: Boolean = true
)

data class RankSettings(
    @JsonProperty("banned_group_id") val bannedGroupId: Int = 3,
    @JsonProperty("mod_group_ids") val modGroupIds: List<Int> = listOf(5, 9),
    @JsonProperty("admin_group_ids") val adminGroupIds: List<Int> = listOf(6, 10, 18, 35),
    @JsonProperty("debug_bypass_group_ids") val debugBypassGroupIds: List<Int> = listOf(11, 34, 40)
)

data class ServerSettings(
    @JsonProperty("debug") val debug: Boolean,
    @JsonProperty("log_level") val logLevel: String
)

data class NetworkSettings(
    @JsonProperty("game_port") val gamePort: Int,
    @JsonProperty("web_port") val webPort: Int,
    @JsonProperty("web_enabled") val webEnabled: Boolean,
    @JsonProperty("connections_per_ip") val connectionsPerIp: Int,
    @JsonProperty("connection_timeout") val connectionTimeout: Int,
    @JsonProperty("packet_limit") val packetLimit: Int,
    @JsonProperty("leak_detection") val leakDetection: String,
)

data class WorldSettings(
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
        // Validate ranges
        if (settings.network.gamePort !in 1..65535) {
            throw IllegalStateException("game_port must be in range 1-65535")
        }
        if (settings.network.webPort !in 1..65535) {
            throw IllegalStateException("web_port must be in range 1-65535")
        }
        if (settings.network.connectionsPerIp < 1) {
            throw IllegalStateException("connections_per_ip must be at least 1")
        }
        if (settings.world.maxPlayers < 1) {
            throw IllegalStateException("max_players must be at least 1")
        }

        // Validate conflicting ports
        val ports = listOf(settings.network.gamePort, settings.network.webPort)
        if (ports.distinct().size != ports.size) {
            throw IllegalStateException("Conflicting ports: game_port and web_port must be unique! (game=${settings.network.gamePort}, web=${settings.network.webPort})")
        }
    }
}
