package net.dodian.uber.net.codec.game

import com.github.michaelbull.logging.InlineLogger
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import net.dodian.uber.net.message.MessageDecoderList

private val logger = InlineLogger()

class GameMessageDecoder(
    private val decoders: MessageDecoderList
) : MessageToMessageDecoder<GamePacket>() {

    override fun decode(ctx: ChannelHandlerContext, packet: GamePacket, out: MutableList<Any>) {
        val decoder = decoders[packet.opcode]
            ?: return logger.debug { "No decoder found for opcode: ${packet.opcode}" }

        out.add(decoder.decode(packet))
    }
}