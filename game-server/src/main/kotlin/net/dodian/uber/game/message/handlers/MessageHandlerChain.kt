package net.dodian.uber.game.message.handlers

import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.net.message.Message

class MessageHandlerChain<M : Message>(
    private val type: Class<M>,
    private val handlers: MutableList<MessageHandler<M>> = mutableListOf()
) {

    fun addHandler(handler: MessageHandler<M>) {
        handlers.add(handler)
    }

    fun notify(player: Player, message: M): Boolean {
        for (handler in handlers) {
            handler.handle(player, message)

            if (message.isTerminated)
                return false
        }

        return true
    }
}