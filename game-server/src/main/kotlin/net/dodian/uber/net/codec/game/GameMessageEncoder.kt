package net.dodian.uber.net.codec.game

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder
import net.dodian.uber.net.message.Message
import net.dodian.uber.net.release.Release

class GameMessageEncoder(
    private val release: Release
) : MessageToMessageEncoder<Message>() {

    override fun encode(ctx: ChannelHandlerContext, message: Message, out: MutableList<Any>) {
        val encoder = release.messageEncoder(message.javaClass) ?: return
        out.add(encoder.encode(message))
    }
}