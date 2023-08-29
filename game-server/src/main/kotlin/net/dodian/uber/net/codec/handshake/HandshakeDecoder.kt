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

        val pipeline = ctx.pipeline()
        val id = buffer.readUnsignedByte().toInt()

        when (id) {
            SERVICE_GAME -> {
                pipeline.addFirst("loginEncoder", LoginEncoder())
                pipeline.addAfter("handshakeDecoder", "loginDecoder", LoginDecoder())
            }
            SERVICE_UPDATE -> println("SERVICE_UPDATE")
        }

        ctx.pipeline().remove(this)
        out.add(HandshakeMessage(id))
    }
}