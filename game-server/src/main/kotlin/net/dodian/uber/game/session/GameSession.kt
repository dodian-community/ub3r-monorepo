package net.dodian.uber.game.session

import com.github.michaelbull.logging.InlineLogger
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import net.dodian.uber.context
import net.dodian.uber.game.MESSAGES_PER_PULSE
import net.dodian.uber.game.message.handlers.MessageHandlerChainSet
import net.dodian.uber.game.message.types.LogoutPacket
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.net.codec.login.LoginRequest
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

private val logger = InlineLogger()

class GameSession(
    channel: Channel,
    private val player: Player,
    private val reconnecting: Boolean
) : Session(channel) {
    private val messages: BlockingQueue<Message> = ArrayBlockingQueue(MESSAGES_PER_PULSE)
    private var request: LoginRequest? = null

    val isReconnecting get() = reconnecting

    override fun destroy() {
        context.handler<PlayerManager>().unregisterPlayer(player)
    }

    fun dispatchMessage(message: Message) {
        if (!channel.isActive || !channel.isOpen)
            return

        val future = channel.writeAndFlush(message)

        if (message::class == LogoutPacket::class)
            future.addListener(ChannelFutureListener.CLOSE)
    }

    fun handlePendingMessages(chainSet: MessageHandlerChainSet) {
        while (messages.isNotEmpty()) {
            val message = messages.poll()

            try {
                chainSet.notify(player, message)
            } catch (cause: Exception) {
                logger.error(cause) { "Uncaught exception thrown while handling message: $message" }
            }
        }
    }

    override fun messageReceived(message: Any) {
        if (messages.size >= MESSAGES_PER_PULSE)
            return logger.warn { "Too many messages in queue for game session, dropping..." }

        messages.add(message as Message)
    }
}