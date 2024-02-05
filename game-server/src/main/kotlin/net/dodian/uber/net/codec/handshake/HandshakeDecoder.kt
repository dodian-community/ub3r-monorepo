package net.dodian.uber.net.codec.handshake

import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import net.dodian.uber.net.codec.login.LoginDecoder
import net.dodian.uber.net.codec.login.LoginEncoder

const val SERVICE_GAME = 14
const val SERVICE_UPDATE = 15

private val logger = InlineLogger()

class HandshakeDecoder : ByteToMessageDecoder() {

    override fun decode(ctx: ChannelHandlerContext, request: ByteBuf, response: MutableList<Any>) {
        logger.debug { "Decoding handshake..." }

        if (!request.isReadable)
            return

        val id = request.readUnsignedByte().toInt()

        logger.debug { "HANDSHAKE ID: $id" }

        when (id) {
            SERVICE_GAME -> {
                ctx.pipeline().addFirst("loginEncoder", LoginEncoder())
                ctx.pipeline().addAfter("handshakeDecoder", "loginDecoder", LoginDecoder())
            }

            SERVICE_UPDATE -> {
                logger.debug { "SERVICE_UPDATE (login opcode: 15) is not implemented - skipping..." }
                return
            }

            else -> {
                val data = request.readBytes(request.readableBytes())
                logger.info { "Unexpected handshake request received. (id=$id, data=$data)" }
                return
            }
        }

        ctx.pipeline().remove(this)
        response.add(HandshakeMessage(id))
    }
}