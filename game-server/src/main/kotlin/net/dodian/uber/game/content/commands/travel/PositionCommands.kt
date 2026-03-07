package net.dodian.uber.game.content.commands.travel

import net.dodian.uber.game.Server
import net.dodian.uber.game.content.commands.CommandContent
import net.dodian.uber.game.content.commands.CommandContext
import net.dodian.uber.game.content.commands.commands
import net.dodian.uber.game.content.commands.isBetaWorld
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.netty.listener.out.SendMessage

object PositionCommands : CommandContent {
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
        client.send(SendMessage("You set your height to ${client.position.z}"))
        return true
    }
    if (context.alias == "godown" && context.specialRights) {
        client.teleportTo(client.position.x, client.position.y, maxOf(client.position.z - 1, 0))
        client.send(SendMessage("You set your height to ${client.position.z}"))
        return true
    }
    if ((context.alias == "bank" || context.alias == "b") && client.playerRights > 1 && net.dodian.utilities.gameWorldId < 2) {
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
            client.send(SendMessage("Welcome to $newPosX, $newPosY at height $newHeight"))
            true
        } catch (_: Exception) {
            context.usage("Wrong usage.. ::${cmd[0]} x y or ::${cmd[0]} x y height")
        }
    }
    if ((context.rawCommand.equals("mypos", true) || context.rawCommand.equals("pos", true)) && (context.specialRights || isBetaWorld())) {
        client.send(SendMessage("Your position is (${client.position.x} , ${client.position.y})"))
        return true
    }
    return false
}
