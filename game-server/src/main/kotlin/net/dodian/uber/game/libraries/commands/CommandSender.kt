package net.dodian.uber.game.libraries.commands

import net.dodian.extensions.sendMessage
import net.dodian.uber.game.libraries.commands.Player

class CommandSender(val player: Player) {
    fun sendMessage(message: String) = player.sendMessage(message)
}