package net.dodian.uber.game.message.handler

import net.dodian.uber.game.modelkt.entity.Player
import net.dodian.uber.net.message.Message
import java.util.*
import java.util.function.Function

class MessageHandlerChainSet(
    private val classes: MutableMap<Class<out Message>, Deque<Class<out Message>>> = mutableMapOf(),
    private val chains: MutableMap<Class<out Message>, MessageHandlerChain<out Message>> = mutableMapOf()
) {

    @Suppress("UNCHECKED_CAST")
    fun <M : Message> notify(player: Player, message: M): Boolean {
        val classes: Deque<Class<out Message>> = classes.computeIfAbsent(message.javaClass) {
            messageClasses(message.javaClass)
        }

        classes.forEach { type ->
            val chain = chains[type] as MessageHandlerChain<in M>?
            if (chain != null && !chain.notify(player, message)) {
                return false
            }
        }

        return true
    }

    @Suppress("UNCHECKED_CAST")
    fun <M : Message> putHandler(clazz: Class<M>, handler: MessageHandler<out Message>) {
        val chain: MessageHandlerChain<M> = chains.computeIfAbsent(clazz) {
            MessageHandlerChain()
        } as MessageHandlerChain<M>

        chain.addHandler(handler as MessageHandler<M>)
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