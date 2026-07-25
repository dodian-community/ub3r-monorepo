package net.dodian.uber.game.engine.systems.interaction.commands

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

class CommandContext(
    val client: Client,
    val rawCommand: String,
    val parts: List<String>,
) {
    val alias: String =
        when {
            rawCommand.startsWith("/") && rawCommand.length > 1 -> "/"
            parts.isEmpty() -> ""
            else -> parts[0].lowercase()
        }

    val specialRights: Boolean
        get() = net.dodian.uber.game.engine.config.rankAdminGroupIds.contains(client.playerGroup)

    fun reply(message: String) {
        client.sendMessage(message)
    }

    fun usage(message: String): Boolean {
        reply(message)
        return true
    }

    fun int(index: Int): Int = parts[index].toInt()

    fun stringFrom(index: Int): String = rawCommand.substringAfterNthSpace(index)

    fun hasArgs(count: Int): Boolean = parts.size > count
}

private fun String.substringAfterNthSpace(index: Int): String {
    var spacesSeen = 0
    for (i in indices) {
        if (this[i] == ' ') {
            spacesSeen++
            if (spacesSeen == index) {
                return substring(i + 1)
            }
        }
    }
    return ""
}