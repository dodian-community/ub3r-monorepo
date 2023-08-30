package net.dodian.uber.game.message.handlers

import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.net.message.Message
import java.util.*

class MessageHandlerChainSet(
    private val chains: MutableMap<Class<out Message>, MessageHandlerChain<out Message>> = mutableMapOf(),
    private val classes: MutableMap<Class<out Message>, Deque<Class<out Message>>> = mutableMapOf()
) {

    @Suppress("UNCHECKED_CAST")
    fun <M : Message> addHandler(clazz: Class<M>, handler: MessageHandler<out Message>) {
        val chain = chains.computeIfAbsent(
            clazz
        ) { MessageHandlerChain(clazz) } as MessageHandlerChain<M>
        chain.addHandler((handler as MessageHandler<M>))
    }

    @Suppress("UNCHECKED_CAST")
    fun <M : Message> notify(player: Player, message: M): Boolean {
        val classes = classes.computeIfAbsent(
            message::class.java
        ) { type: Class<out Message> -> this.messageClasses(type) }

        for (type in classes) {
            val chain = chains[type] as MessageHandlerChain<in M>
            if (!chain.notify(player, message))
                return false
        }

        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun <M : Message> messageClasses(type: Class<M>): Deque<Class<out Message>> {
        val classes: Deque<Class<out Message>> = ArrayDeque()
        var clazz: Class<in M> = type

        do {
            classes.addFirst(clazz as Class<out Message>)
            clazz = clazz.getSuperclass()
        } while (clazz != Message::class.java)

        return classes
    }
}