package net.dodian.uber.game.session

import com.github.michaelbull.logging.InlineLogger
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.AttributeKey
import io.netty.util.ReferenceCountUtil
import net.dodian.uber.net.codec.handshake.HandshakeMessage
import net.dodian.uber.net.codec.handshake.SERVICE_GAME
import net.dodian.uber.net.codec.handshake.SERVICE_UPDATE
import net.dodian.uber.net.codec.jaggrab.JagGrabRequest
import java.net.http.HttpRequest

private val logger = InlineLogger()

val SESSION_KEY: AttributeKey<Session> = AttributeKey.valueOf("session")

@Sharable
class GameHandler : ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, message: Any) {
        try {
            val ch = ctx.channel()
            val attribute = ch.attr(SESSION_KEY)
            var session = attribute.get()

            if (message is HttpRequest || message is JagGrabRequest)
                session = UpdateSession(ch)

            if (session != null) {
                session.messageReceived(message)
                return
            }

            if (message is HandshakeMessage) {
                when (message.serviceId) {
                    SERVICE_GAME -> attribute.set(LoginSession(ch))
                    SERVICE_UPDATE -> attribute.set(UpdateSession(ch))
                }
            }
        } finally {
            ReferenceCountUtil.release(message)
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        val ch = ctx.channel()

        ch.attr(SESSION_KEY).getAndSet(null)?.destroy()

        logger.debug { "Channel disconnected: $ch" }
        ch.close()
    }

    @Deprecated("Deprecated in Java")
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        val message = cause.message
        if (message != null && !message.contains("An existing connection was forcibly closed by the remote host")) {
            logger.warn { "Exception occurred for channel: ${ctx.channel()}, closing..." }
        }
        ctx.channel().close()
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }
}