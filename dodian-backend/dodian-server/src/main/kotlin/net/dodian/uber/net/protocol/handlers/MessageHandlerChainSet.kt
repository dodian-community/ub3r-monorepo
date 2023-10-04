package net.dodian.uber.net.protocol.handlers

import net.dodian.context
import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.uber.net.message.Message
import net.dodian.uber.net.protocol.packets.client.ButtonMessage
import net.dodian.uber.net.protocol.packets.client.WalkMessage
import java.util.*
import kotlin.reflect.KClass

@Suppress("MemberVisibilityCanBePrivate")
class MessageHandlerChainSet(
    private val classes: MutableMap<Class<out Message>, Deque<Class<out Message>>> = mutableMapOf(),
    private val chains: MutableMap<Class<out Message>, MessageHandlerChain<out Message>> = mutableMapOf()
) {

    init {
        val world = context.world

        putHandler(WalkMessage::class, WalkMessageHandler(world))
        putHandler(ButtonMessage::class, ButtonMessageHandler(world))
    }

    @Suppress("UNCHECKED_CAST")
    fun <M : Message> notify(player: Player, message: M): Boolean {
        val classes: Deque<Class<out Message>> = classes.computeIfAbsent(message.javaClass) {
            messageClasses(message.javaClass)
        }

        classes.forEach { type ->
            val chain = chains[type] as MessageHandlerChain<in M>?
            if (chain != null && !chain.notify(player, message))
                return false
        }

        return true
    }

    fun <M : Message> putHandler(clazz: KClass<M>, handler: MessageHandler<out Message>) {
        putHandler(clazz.java, handler)
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