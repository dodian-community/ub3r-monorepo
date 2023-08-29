package net.dodian.uber.net.protocol

import net.dodian.uber.net.protocol.packet.Packet
import net.dodian.uber.net.protocol.packet.PacketCodec

class Protocol(codecs: Set<PacketCodec<*>>) {
    private val decoders = codecs.associateBy { it.opcode }
    private val encoders = codecs.associateBy { it.type }

    fun decoder(opcode: Int) = decoders[opcode]
    
    @Suppress("UNCHECKED_CAST")
    fun <T : Packet> encoder(type: Class<T>) = encoders[type] as? PacketCodec<T>
}