package net.dodian.uber.net.codec.login

import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

private val logger = InlineLogger()

class LoginEncoder : MessageToByteEncoder<LoginResponse>(LoginResponse::class.java) {

    override fun encode(ctx: ChannelHandlerContext, response: LoginResponse, out: ByteBuf) {
        logger.debug { response }

        out.writeByte(response.status)

        if (response.status == STATUS_OK) {
            out.writeByte(response.rights)
            out.writeByte(if (response.isFlagged) 1 else 0)
        }
    }
}