package net.dodian.uber.web.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.web.models.AvailableCommands
import net.dodian.uber.web.models.OnlinePlayer
import net.dodian.uber.web.models.ServerStatus

fun Route.gameRoutes() {
    route("/game") {
        get("/status") {
            call.respond(ServerStatus())
        }

        get("/commands") {
            call.respond(AvailableCommands())
        }

        authenticate("auth-jwt") {
            get("/players") {
                call.respond(PlayerHandler.players.filterNotNull().map {
                    OnlinePlayer(
                        username = it.playerName,
                        rightsFlags = it.rightsFlags,
                        position = it.position
                    )
                })
            }
        }
    }
}