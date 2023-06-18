package net.dodian.config

import io.ktor.http.*
import io.netty.handler.codec.http.HttpScheme

data class InstanceConfig(
    val server: ServerConfig = ServerConfig(),
    val game: GameConfig = GameConfig(),
    val groups: GroupsConfig = GroupsConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val webApi: WebApiConfig = WebApiConfig()
)

data class ServerConfig(
    val name: String = "Uber Server",
    val port: Int = 43594,
    val debugMode: Boolean = false,
    val environment: String = Environment.Production.name
)

data class GameConfig(
    val worldId: Int = 1,
    val connectionsPerIp: Int = 2,
    val clientVersion: String = "ub3r_client",

    val xpMultiplierGlobal: Double = 1.0,

    val forumFeaturesEnabled: Boolean = false
)

data class GroupsConfig(
    val banned: List<Int> = listOf(),
    val muted: List<Int> = listOf(),
    val tradeLocked: List<Int> = listOf(),

    val premium: List<Int> = listOf(),
    val betaTester: List<Int> = listOf(),
    val retiredStaff: List<Int> = listOf(),

    val trialModerator: List<Int> = listOf(),
    val moderator: List<Int> = listOf(),
    val developer: List<Int> = listOf(),
    val administrator: List<Int> = listOf(),
    val hiddenAdministrator: List<Int> = listOf(),
)

data class WebApiConfig(
    val port: Int = 8080,
    val scheme: String = "http",
    val hostname: String = "127.0.0.1",
    val jwtAudience: String = "dodian-net",
    val jwtDomain: String = "$scheme://$hostname:$port/",
    val jwtRealm: String = "Dodian Game API",
    val jwtSecret: String = "dodian-super-secret-jwt-secret"
)

data class DatabaseConfig(
    val host: String = "localhost",
    val port: Int = 3306,
    val name: String = "dodiannet",
    val user: String = "dodian",
    val pass: String = "abcd1234",
    val tablePrefix: String = "",
    val initialize: Boolean = false
)

enum class Environment {
    Development,
    Staging,
    Production
}

// TODO: Implement this?
enum class Datastore {
    MySQL,
    FlatFile
}