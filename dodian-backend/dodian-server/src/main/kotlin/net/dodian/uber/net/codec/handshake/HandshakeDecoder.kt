package net.dodian.uber.net.codec.handshake

import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import net.dodian.uber.net.codec.login.LoginDecoder
import net.dodian.uber.net.codec.login.LoginEncoder
import net.dodian.uber.net.codec.update.UpdateDecoder
import net.dodian.uber.net.codec.update.UpdateEncoder

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
                ctx.pipeline().addFirst("updateEncoder", UpdateEncoder())
                ctx.pipeline().addBefore("handler", "updateDecoder", UpdateDecoder())

                val buf = ctx.alloc().buffer(8).writeLong(0)
                ctx.channel().writeAndFlush(buf)
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