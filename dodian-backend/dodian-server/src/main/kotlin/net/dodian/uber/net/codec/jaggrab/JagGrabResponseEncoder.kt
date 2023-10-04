package net.dodian.uber.net.codec.jaggrab

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

class JagGrabResponseEncoder : MessageToMessageEncoder<JagGrabResponse>() {

    override fun encode(ctx: ChannelHandlerContext, response: JagGrabResponse, out: MutableList<Any>) {
        out.add(response.fileData)
    }
}