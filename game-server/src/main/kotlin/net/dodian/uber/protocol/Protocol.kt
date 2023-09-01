package net.dodian.uber.protocol

import net.dodian.uber.protocol.packet.Packet
import net.dodian.uber.protocol.packet.PacketCodec

class Protocol(codecs: Set<PacketCodec<*>>) {
    private val decoders = codecs.associateBy { it.opcode }
    private val encoders = codecs.associateBy { it.type }

    fun getDecoder(opcode: Int): PacketCodec<*>? {
        return decoders[opcode]
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Packet> getEncoder(type: Class<T>): PacketCodec<T>? {
        return encoders[type] as? PacketCodec<T>
    }
}