package net.dodian.uber.net.release

import net.dodian.uber.net.message.Message
import net.dodian.uber.net.meta.PacketMetaData
import net.dodian.uber.net.meta.PacketMetaDataGroup

abstract class Release(
    private val incomingPacketMetaData: PacketMetaDataGroup = PacketMetaDataGroup(),
    private val decoders: Array<MessageDecoder<*>?> = Array(256) { null },
    private val encoders: MutableMap<Class<out Message>, MessageEncoder<*>> = mutableMapOf()
) {

    @Suppress("UNCHECKED_CAST")
    fun <M : Message> messageEncoder(type: Class<M>): MessageEncoder<M>? {
        return encoders[type] as MessageEncoder<M>?
    }

    fun messageDecoder(opcode: Int): MessageDecoder<*>? {
        return decoders[opcode]
    }

    fun incomingPacketMetaData(opcode: Int): PacketMetaData {
        return incomingPacketMetaData.metaData(opcode) ?: error("No metadata found for opcode: $opcode")
    }
}