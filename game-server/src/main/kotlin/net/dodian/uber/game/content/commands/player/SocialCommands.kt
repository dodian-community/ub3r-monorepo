package net.dodian.uber.game.content.commands.player

import net.dodian.uber.game.systems.interaction.commands.*

import net.dodian.uber.game.Server
import net.dodian.uber.game.systems.interaction.commands.CommandContent
import net.dodian.uber.game.systems.interaction.commands.CommandContext
import net.dodian.uber.game.systems.interaction.commands.commands
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.audit.ChatLog

object SocialCommands : CommandContent {
    override fun definitions() =
        commands {
            command("/", "yell", "mod") {
                handleSocial(this)
            }
        }
}

private fun handleSocial(context: CommandContext): Boolean {
    val client = context.client
    val command = context.rawCommand
    val cmd = context.parts
    if ((command.startsWith("/") && command.substring(1).isNotEmpty()) || (context.alias == "yell" && command.length > 5)) {
        if (!client.premium) {
            client.sendMessage("You must be a Premium Member to yell.")
            client.sendMessage("Use the Dodian.net Market Forums to post new threads to buy/sell.")
            return true
        }
        if (!Server.chatOn && client.playerRights < 1) {
            client.sendMessage("Yell chat is disabled!")
            return true
        }
        var text = command.substring(if (command.startsWith("/")) 1 else 5)
        text = text.replace("<col", "<moo").replace("<shad", "<moo")
        text = text.replace("b:", "<col=292BA3>").replace("r:", "<col=FF0000>")
        text = text.replace("p:", "<col=FF00FF>").replace("o:", "<col=FF8000>")
        text = text.replace("g:", "<col=0B610B>").replace("y:", "<col=FFFF00>")
        text = text.replace("d:", "<col=000000>")
        if (!client.isMuted) {
            for (blocked in arrayOf("chalreq", "duelreq", "tradereq")) {
                if (text.contains(blocked)) {
                    return true
                }
            }
            val yell = text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            when {
                client.playerRights == 0 -> client.yell("[YELL]<col=000000>${client.playerName}<col=0000ff>: $yell")
                client.playerRights == 1 -> client.yell("<col=0B610B>${client.playerName}<col=000000>: <col=0B610B>$yell@cr1@")
                else -> client.yell("<col=FFFF00>${client.playerName}<col=000000>: <col=0B610B>$yell@cr2@")
            }
            ChatLog.recordYellChat(client, yell)
        } else {
            client.sendMessage("You are currently muted!")
        }
        return true
    }
    if (command.startsWith("mod") && client.playerRights > 0) {
        val text = command.substring(cmd[0].length + 1).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        client.modYell("[STAFF] ${client.playerName}: $text")
        ChatLog.recordModChat(client, text)
        return true
    }
    return false
}
