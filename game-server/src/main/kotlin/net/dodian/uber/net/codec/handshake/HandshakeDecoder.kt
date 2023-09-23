package net.dodian.uber.net.codec.handshake

import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import net.dodian.uber.net.codec.login.LoginDecoder
import net.dodian.uber.net.codec.login.LoginEncoder

private val logger = InlineLogger()

class HandshakeDecoder : ByteToMessageDecoder() {

    override fun decode(ctx: ChannelHandlerContext, buffer: ByteBuf, out: MutableList<Any>) {
        if (!buffer.isReadable)
            return

        val id = buffer.readUnsignedByte().toInt()

        when (id) {
            SERVICE_GAME -> {
                ctx.pipeline().addFirst("loginEncoder", LoginEncoder())
                ctx.pipeline().addAfter("handshakeDecoder", "loginDecoder", LoginDecoder())
            }

            SERVICE_UPDATE -> {
                logger.debug { "Supposed to do client updating stuff..." }
            }

            else -> {
                val data = buffer.readBytes(buffer.readableBytes())
                logger.info { "Unexpected handshake request received. (id=$id, data=$data)" }
                return
            }
        }

        ctx.pipeline().remove(this)
        out.add(HandshakeMessage(id))
    }
}