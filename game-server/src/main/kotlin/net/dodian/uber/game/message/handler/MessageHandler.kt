package net.dodian.uber.game.message.handler

import net.dodian.uber.game.modelkt.World
import net.dodian.uber.game.modelkt.entity.Player
import net.dodian.uber.net.message.Message

abstract class MessageHandler<M : Message>(
    val world: World
) {
    abstract fun handle(player: Player, message: M)
}