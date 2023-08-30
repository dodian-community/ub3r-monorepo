package net.dodian.uber.net.codec.game

import com.github.michaelbull.logging.InlineLogger
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import net.dodian.uber.net.release.MessageDecoder
import net.dodian.uber.net.release.Release

private val logger = InlineLogger()

class GameMessageDecoder(
    val release: Release
) : MessageToMessageDecoder<GamePacket>() {

    override fun decode(ctx: ChannelHandlerContext, packet: GamePacket, out: MutableList<Any>) {
        val decoder: MessageDecoder<*>? = release.messageDecoder(packet.opcode)
        if (decoder != null) out.add(decoder.decode(packet))
        else logger.debug { "Unidentified packet received - opcode: ${packet.opcode}" }
    }
}