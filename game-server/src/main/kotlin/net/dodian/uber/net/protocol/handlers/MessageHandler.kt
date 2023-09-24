package net.dodian.uber.net.protocol.handlers

import net.dodian.uber.game.modelkt.World
import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.uber.net.message.Message

abstract class MessageHandler<M : Message>(
    val world: World
) {
    abstract fun handle(player: Player, message: M)
}