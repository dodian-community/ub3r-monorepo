package net.dodian.uber.web.models

import net.dodian.config.Environment
import net.dodian.config.gameConfig
import net.dodian.config.serverConfig
import net.dodian.config.serverEnvironment
import net.dodian.uber.commandsLibrary
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.utilities.RightsFlag
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class ServerStatus(
    val playersOnline: Int = PlayerHandler.playersOnline.size,
    val uptime: Long = ChronoUnit.SECONDS.between(Server.serverStartup, LocalDateTime.now()),
    val debugMode: Boolean = serverConfig.debugMode,
    val environment: Environment = serverEnvironment,
    val worldId: Int = gameConfig.worldId
)

data class AvailableCommands(
    val commands: List<CommandDescription> = commandsLibrary.commands.values.map {
        CommandDescription(it.name, it.description, it.usage, it.permissions)
    }
)

data class ApiError(
    val message: String,
    val defaultScheme: String,
    val realm: String
)

data class CommandDescription(
    val name: String,
    val description: String,
    val usage: String,
    val rights: List<RightsFlag>?
)

data class User(
    val username: String,
    val password: String
)

data class OnlinePlayer(
    val username: String,
    val rightsFlags: List<RightsFlag>,
    val position: Position
)