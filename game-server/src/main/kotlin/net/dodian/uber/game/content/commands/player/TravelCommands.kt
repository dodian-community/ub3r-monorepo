package net.dodian.uber.game.content.commands.player

import net.dodian.uber.game.systems.content.commands.*

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.config.gameWorldId
import net.dodian.uber.game.model.Position

object TravelCommands : CommandContent {
    override fun definitions() =
        commands {
            command("goup", "godown", "bank", "b", "tele", "mypos", "pos", "travel") {
                handlePosition(this)
            }
        }
}

private fun handlePosition(context: CommandContext): Boolean {
    val client = context.client
    val cmd = context.parts
    if (context.alias == "goup" && context.specialRights) {
        client.teleportTo(client.position.x, client.position.y, client.position.z + 1)
        client.sendMessage("You set your height to ${client.position.z}")
        return true
    }
    if (context.alias == "godown" && context.specialRights) {
        client.teleportTo(client.position.x, client.position.y, maxOf(client.position.z - 1, 0))
        client.sendMessage("You set your height to ${client.position.z}")
        return true
    }
    if ((context.alias == "bank" || context.alias == "b") && client.playerRights > 1 && gameWorldId < 2) {
        client.openUpBank()
        return true
    }
    if (context.specialRights && (context.alias == "bank" || context.alias == "b")) {
        client.openUpBank()
        return true
    }
    if (context.alias == "travel" && context.specialRights) {
        client.setTravelMenu()
        return true
    }
    if (context.alias == "tele" && (context.specialRights || isBetaWorld())) {
        return try {
            val newPosX = cmd[1].toInt()
            val newPosY = cmd[2].toInt()
            val newHeight = if (cmd.size != 4) 0 else cmd[3].toInt()
            client.transport(Position(newPosX, newPosY, newHeight))
            client.sendMessage("Welcome to $newPosX, $newPosY at height $newHeight")
            true
        } catch (_: Exception) {
            context.usage("Wrong usage.. ::${cmd[0]} x y or ::${cmd[0]} x y height")
        }
    }
    if ((context.rawCommand.equals("mypos", true) || context.rawCommand.equals("pos", true)) && (context.specialRights || isBetaWorld())) {
        client.sendMessage("Your position is (${client.position.x} , ${client.position.y})")
        return true
    }
    return false
}
