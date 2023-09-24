package net.dodian.uber.session

import com.github.michaelbull.logging.InlineLogger
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import net.dodian.context
import net.dodian.uber.game.MESSAGES_PER_PULSE
import net.dodian.uber.net.protocol.handlers.MessageHandlerChainSet
import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.uber.net.message.Message
import net.dodian.uber.net.protocol.packets.server.LogoutMessage
import net.dodian.uber.services.GameService
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

private val logger = InlineLogger()

class GameSession(
    channel: Channel,
    val player: Player,
    val reconnecting: Boolean
) : Session(channel) {
    private val messages: BlockingQueue<Message> = ArrayBlockingQueue(MESSAGES_PER_PULSE)

    override fun destroy() {
        context.service<GameService>().unregisterPlayer(player)
    }

    override fun messageReceived(message: Any) {
        if (messages.size >= MESSAGES_PER_PULSE)
            return logger.warn { "Too many messages in queue for game session, dropping..." }

        messages.add(message as Message)
    }

    fun handlePendingMessages(chainSet: MessageHandlerChainSet) {
        while (messages.isNotEmpty()) {
            chainSet.notify(player, messages.poll())
        }
    }

    fun handlePlayerSaverResponse(success: Boolean) {
        context.service<GameService>().finalizePlayerUnregistration(player)
    }

    fun dispatchMessage(message: Message) {
        if (!channel.isActive || !channel.isOpen)
            return

        val future = channel.writeAndFlush(message)
        if (message is LogoutMessage) {
            logger.debug { "Message was logout, adding future close listener..." }
            future.addListeners(ChannelFutureListener.CLOSE)
        }
    }
}