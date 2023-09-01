package net.dodian.uber.net.builder.upstream

import io.netty.buffer.ByteBuf
import net.dodian.uber.protocol.packet.FixedLengthPacketCodec
import net.dodian.uber.protocol.packet.UpstreamDiscardPacket
import net.dodian.uber.protocol.packet.UpstreamPacket
import org.openrs2.crypto.StreamCipher

class UpstreamFixedLengthPacketCodec<T : UpstreamPacket>(
    type: Class<T>,
    opcode: Int,
    length: Int,
    private val decoder: (ByteBuf) -> UpstreamPacket
) : FixedLengthPacketCodec<T>(type, opcode, length) {

    @Suppress("UNCHECKED_CAST")
    override fun decode(buf: ByteBuf, cipher: StreamCipher): T {
        val packet = decoder.invoke(buf) as T
        if (buf.isReadable && packet is UpstreamDiscardPacket) {
            buf.readBytes(buf.readableBytes())
        }
        return packet
    }

    override fun encode(packet: T, buf: ByteBuf, cipher: StreamCipher) {
        throw NotImplementedError("Upstream packet cannot be encoded.")
    }
}
