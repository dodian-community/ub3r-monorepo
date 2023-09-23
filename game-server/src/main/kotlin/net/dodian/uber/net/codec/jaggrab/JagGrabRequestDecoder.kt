package net.dodian.uber.net.codec.jaggrab

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder

class JagGrabRequestDecoder : MessageToMessageDecoder<String>() {
    override fun decode(ctx: ChannelHandlerContext, request: String, out: MutableList<Any>) {
        if (request.startsWith("JAGGRAB /"))
            out.add(JagGrabRequest(request.substring(8).trim()))
        else error("Corrupted request line.")
    }
}