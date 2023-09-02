package net.dodian.uber.net.builder.upstream

import io.netty.buffer.ByteBuf
import net.dodian.uber.protocol.packet.UpstreamPacket

private const val VARIABLE_BYTE_LENGTH = -1
private const val VARIABLE_SHORT_LENGTH = -2

@DslMarker
private annotation class PacketBuilderDsl

@PacketBuilderDsl
class UpstreamPacketBuilder<T : UpstreamPacket> {
    private lateinit var decoder: (ByteBuf) -> T

    var opcode: Int = -1
    var length: Int = 0

    val variableByteLength: Int
        get() = VARIABLE_BYTE_LENGTH

    val variableShortLength: Int
        get() = VARIABLE_SHORT_LENGTH

    fun decode(decoder: (buf: ByteBuf) -> T) {
        this.decoder = decoder
    }

    fun build(): UpstreamPacketStructure<T> {
        check(opcode != -1)
        check(::decoder.isInitialized)
        return UpstreamPacketStructure(opcode, length, decoder)
    }
}