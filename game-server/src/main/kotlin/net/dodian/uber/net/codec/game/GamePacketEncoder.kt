package net.dodian.uber.net.codec.game

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import net.dodian.uber.net.meta.PacketType
import net.dodian.utilities.security.IsaacRandom

class GamePacketEncoder(
    val random: IsaacRandom
) : MessageToByteEncoder<GamePacket>() {

    override fun encode(ctx: ChannelHandlerContext, packet: GamePacket, out: ByteBuf) {
        val type = packet.type
        val payloadLength = packet.length

        when {
            type == PacketType.VARIABLE_BYTE && payloadLength >= 256 ->
                error("Payload too long for variable byte packet.")

            type == PacketType.VARIABLE_SHORT && payloadLength >= 65_536 ->
                error("Payload too long for variable short packet.")
        }

        out.writeByte(packet.opcode + random.nextInt() and 0xFF)
        when (type) {
            PacketType.VARIABLE_BYTE -> out.writeByte(payloadLength)
            PacketType.VARIABLE_SHORT -> out.writeShort(payloadLength)
            else -> {}
        }

        out.writeBytes(packet.payload)
    }
}