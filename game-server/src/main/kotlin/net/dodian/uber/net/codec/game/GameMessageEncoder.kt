package net.dodian.uber.net.codec.game

import com.github.michaelbull.logging.InlineLogger
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder
import net.dodian.uber.net.message.Message
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.message.MessageEncoderList

private val logger = InlineLogger()
class GameMessageEncoder(
    private val encoders: MessageEncoderList
) : MessageToMessageEncoder<Message>() {

    @Suppress("UNCHECKED_CAST")
    override fun encode(ctx: ChannelHandlerContext, message: Message, out: MutableList<Any>) {
        val encoder = encoders[message::class]
            ?: return logger.debug { "No encoder found for message type: ${message::class.simpleName}" }

        out.add((encoder as MessageEncoder<Message>).encode(message))
    }
}