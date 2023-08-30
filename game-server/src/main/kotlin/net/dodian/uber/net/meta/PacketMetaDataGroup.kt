package net.dodian.uber.net.meta

import com.google.common.base.Preconditions

class PacketMetaDataGroup(
    private val packets: Array<PacketMetaData?> = Array(256) { null }
) {

    fun metaData(opcode: Int): PacketMetaData? {
        Preconditions.checkElementIndex(opcode, packets.size, "Opcode out of bounds.")
        return packets[opcode]
    }
}