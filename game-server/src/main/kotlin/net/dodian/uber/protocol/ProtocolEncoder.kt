package net.dodian.uber.protocol

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import net.dodian.uber.protocol.packet.Packet
import net.dodian.uber.protocol.packet.PacketCodec
import net.dodian.utilities.security.IsaacRandom

class ProtocolEncoder(
    val protocol: Protocol,
    val random: IsaacRandom
) : MessageToByteEncoder<Packet>(Packet::class.java) {

    override fun encode(ctx: ChannelHandlerContext, msg: Packet, out: ByteBuf) {
        val encoder = msg.encoder()
        out.writeByte(encoder.opcode + random.nextInt())

        val lengthWriterIndex = out.writerIndex()
        encoder.offsetLength(out)

        val payloadWriterIndex = out.writerIndex()
        encoder.encode(msg, out, random)

        val payloadLength = out.writerIndex() - payloadWriterIndex
        encoder.setLength(out, lengthWriterIndex, payloadLength)
    }

    override fun allocateBuffer(ctx: ChannelHandlerContext, msg: Packet, preferDirect: Boolean): ByteBuf {
        val encoder = msg.encoder()
        return encoder.allocEncodeBuffer(ctx.alloc(), msg, preferDirect)
    }

    private fun <T : Packet> T.encoder(): PacketCodec<T> {
        return protocol.getEncoder(javaClass) ?: error("Encoder not found for packet type $javaClass")
    }
}