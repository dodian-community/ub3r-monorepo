package net.dodian.uber.net.protocol

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import net.dodian.uber.net.protocol.packet.Packet
import net.dodian.uber.net.protocol.packet.PacketCodec
import org.openrs2.crypto.NopStreamCipher
import org.openrs2.crypto.StreamCipher

class ProtocolEncoder(
    var protocol: Protocol,
    var cipher: StreamCipher = NopStreamCipher
) : MessageToByteEncoder<Packet>(Packet::class.java) {
    override fun encode(ctx: ChannelHandlerContext, msg: Packet, out: ByteBuf) {
        val encoder = msg.encoder()
        out.writeByte(encoder.opcode + cipher.nextInt())

        val lengthWriterIndex = out.writerIndex()
        encoder.offsetLength(out)

        val payloadWriterIndex = out.writerIndex()
        encoder.encode(msg, out, cipher)

        val payloadLength = out.writerIndex() - payloadWriterIndex
        encoder.setLength(out, lengthWriterIndex, payloadLength)
    }

    private fun <T : Packet> T.encoder(): PacketCodec<T> =
        protocol.encoder(javaClass) ?: error("Encoder not found for packet type (type=$javaClass).")
}