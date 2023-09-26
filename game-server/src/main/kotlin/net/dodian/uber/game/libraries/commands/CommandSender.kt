package net.dodian.uber.game.libraries.commands

import net.dodian.uber.game.modelkt.entity.player.Player

class CommandSender(val player: Player) {
    fun sendMessage(message: String) = player.sendMessage(message)
}