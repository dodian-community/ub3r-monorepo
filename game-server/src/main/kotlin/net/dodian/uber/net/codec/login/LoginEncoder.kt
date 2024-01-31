package net.dodian.uber.net.codec.login

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class LoginEncoder : MessageToByteEncoder<LoginResponse>(LoginResponse::class.java) {
    override fun encode(ctx: ChannelHandlerContext, response: LoginResponse, out: ByteBuf) {
        out.writeByte(response.status)

        if (response.status == STATUS_OK) {
            out.writeByte(response.rights)
            out.writeByte(if (response.flagged) 1 else 0)
        }
    }
}