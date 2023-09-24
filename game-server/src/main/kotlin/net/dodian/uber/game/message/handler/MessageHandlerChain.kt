package net.dodian.uber.game.message.handler

import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.uber.net.message.Message

class MessageHandlerChain<M : Message>(
    private val handlers: MutableList<MessageHandler<M>> = mutableListOf()
) {

    fun addHandler(handler: MessageHandler<M>) {
        handlers.add(handler)
    }

    fun notify(player: Player, message: M): Boolean {
        handlers.forEach {
            it.handle(player, message)
            if (message.isTerminated)
                return false
        }

        return true
    }
}