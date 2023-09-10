package net.dodian.uber.net.builder.downstream

import io.netty.buffer.ByteBuf
import net.dodian.uber.protocol.packet.DownstreamPacket
import net.dodian.uber.protocol.packet.FixedLengthPacketCodec
import net.dodian.utilities.security.IsaacRandom

public class DownstreamFixedLengthPacketCodec<T : DownstreamPacket>(
    type: Class<T>,
    opcode: Int,
    length: Int,
    private val encoder: (T, ByteBuf) -> Unit
) : FixedLengthPacketCodec<T>(type, opcode, length) {

    override fun decode(buf: ByteBuf, random: IsaacRandom): T {
        throw NotImplementedError("Downstream packet cannot be decoded.")
    }

    override fun encode(packet: T, buf: ByteBuf, random: IsaacRandom) {
        encoder.invoke(packet, buf)
    }
}
