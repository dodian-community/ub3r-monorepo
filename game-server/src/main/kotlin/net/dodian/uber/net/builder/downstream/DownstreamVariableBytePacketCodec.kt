package net.dodian.uber.net.builder.downstream

import io.netty.buffer.ByteBuf
import net.dodian.uber.protocol.packet.DownstreamPacket
import net.dodian.uber.protocol.packet.VariableByteLengthPacketCodec
import net.dodian.utilities.security.IsaacRandom
import org.openrs2.crypto.StreamCipher

class DownstreamVariableBytePacketCodec<T : DownstreamPacket>(
    type: Class<T>,
    opcode: Int,
    private val encoder: (T, ByteBuf) -> Unit
) : VariableByteLengthPacketCodec<T>(type, opcode) {

    override fun decode(buf: ByteBuf, random: IsaacRandom): T {
        throw NotImplementedError("Downstream packet cannot be decoded.")
    }

    override fun encode(packet: T, buf: ByteBuf, random: IsaacRandom) {
        encoder.invoke(packet, buf)
    }
}
