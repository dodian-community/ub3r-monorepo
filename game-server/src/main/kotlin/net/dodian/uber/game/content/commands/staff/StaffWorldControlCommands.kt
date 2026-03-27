package net.dodian.uber.game.content.commands.staff

import net.dodian.uber.game.Server
import net.dodian.uber.game.content.commands.CommandContent
import net.dodian.uber.game.content.commands.CommandContext
import net.dodian.uber.game.content.commands.commands
import net.dodian.uber.game.content.commands.recordStaffCommand
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces

object StaffWorldControlCommands : CommandContent {
    override fun definitions() =
        commands {
            command("toggleyell", "togglepvp", "toggletrade", "toggleduel", "toggledrop", "toggleshop", "togglebank") {
                handleWorldControl(this)
            }
        }
}

private fun handleWorldControl(context: CommandContext): Boolean {
    val client = context.client
    if (client.playerRights <= 0) {
        return false
    }
    val toggleCommand = if (context.alias.startsWith("toggle")) context.alias.replace("_", "") else context.alias
    when {
        toggleCommand.equals("toggleyell", true) -> {
            Server.chatOn = !Server.chatOn
            client.yell(if (Server.chatOn) "[SERVER]: Yell has been enabled!" else "[SERVER]: Yell has been disabled!")
        }
        toggleCommand.equals("togglepvp", true) -> {
            Server.pking = !Server.pking
            client.yell(if (Server.pking) "[SERVER]: Player Killing has been enabled!" else "[SERVER]: Player Killing  has been disabled!")
        }
        toggleCommand.equals("toggletrade", true) -> {
            Server.trading = !Server.trading
            client.yell(if (Server.trading) "[SERVER]: Trading has been enabled!" else "[SERVER]: Trading has been disabled!")
        }
        toggleCommand.equals("toggleduel", true) -> {
            Server.dueling = !Server.dueling
            client.yell(if (Server.dueling) "[SERVER]: Dueling has been enabled!" else "[SERVER]: Dueling has been disabled!")
        }
        toggleCommand.equals("toggledrop", true) -> {
            Server.dropping = !Server.dropping
            client.yell(if (Server.dropping) "[SERVER]: Dropping items has been enabled!" else "[SERVER]: Dropping items has been disabled!")
        }
        toggleCommand.equals("toggleshop", true) -> {
            Server.shopping = !Server.shopping
            client.yell(if (Server.shopping) "[SERVER]: Shops has been enabled!" else "[SERVER]: Shops has been disabled!")
        }
        toggleCommand.equals("togglebank", true) -> {
            Server.banking = !Server.banking
            client.yell(if (Server.banking) "[SERVER]: The Bank has been enabled!" else "[SERVER]: The Bank has been disabled!")
            if (!Server.banking) {
                for (player in PlayerHandler.players) {
                    val other = player as? Client ?: continue
                    if (other.IsBanking) {
                        other.send(RemoveInterfaces())
                        other.IsBanking = false
                    }
                }
            }
        }
        else -> return false
    }
    recordStaffCommand(client, context.rawCommand)
    return true
}
