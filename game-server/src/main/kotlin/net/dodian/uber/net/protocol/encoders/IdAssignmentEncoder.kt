package net.dodian.uber.net.protocol.encoders

import net.dodian.uber.net.codec.game.*
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.IdAssignment

class IdAssignmentEncoder : MessageEncoder<IdAssignment>() {

    override fun encode(message: IdAssignment): GamePacket {
        val builder = GamePacketBuilder(249)
        builder.put(DataType.BYTE, DataTransformation.ADD, if (message.isPremium) 1 else 0)
        builder.put(DataType.SHORT, DataOrder.LITTLE, DataTransformation.ADD, message.id)
        return builder.toGamePacket()
    }
}