package net.dodian.uber.net.codec.game

import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import net.dodian.uber.net.message.meta.PacketType
import net.dodian.utilities.security.IsaacRandom

private val logger = InlineLogger()

class GamePacketEncoder(
    private val random: IsaacRandom
) : MessageToByteEncoder<GamePacket>() {

    override fun encode(ctx: ChannelHandlerContext, packet: GamePacket, out: ByteBuf) {
        val type = packet.type
        val length = packet.length

        if (type == PacketType.VARIABLE_BYTE && length >= 256)
            error("Payload too long for variable byte packet.")
        else if (type == PacketType.VARIABLE_SHORT && length >= 65_536)
            error("Payload too long for variable short packet.")

        out.writeByte(packet.opcode + random.nextInt() and 0xFF)

        when (type) {
            PacketType.VARIABLE_BYTE -> out.writeByte(length)
            PacketType.VARIABLE_SHORT -> out.writeShort(length)
            else -> {}
        }
        out.writeBytes(packet.payload)
    }
}