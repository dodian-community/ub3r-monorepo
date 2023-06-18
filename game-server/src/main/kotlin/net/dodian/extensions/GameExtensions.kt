package net.dodian.extensions

import net.dodian.uber.game.libraries.commands.Player
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage

fun Player.sendMessage(message: String) = send(SendMessage(message))