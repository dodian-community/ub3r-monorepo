package net.dodian.uber.game.message.handlers

import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.net.message.Message

abstract class MessageHandler<M : Message> {
    abstract fun handle(player: Player, message: M)
}