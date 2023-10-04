package net.dodian.uber.net.protocol.encoders.region

import net.dodian.uber.net.codec.game.*
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.region.SendObjectMessage

class SendObjectMessageEncoder : MessageEncoder<SendObjectMessage>() {
    override fun encode(message: SendObjectMessage): GamePacket {
        val builder = GamePacketBuilder(151)
        builder.put(DataType.BYTE, DataTransformation.ADD, message.offset)
        builder.put(DataType.SHORT, DataOrder.LITTLE, message.id)
        builder.put(DataType.BYTE, DataTransformation.SUBTRACT, message.type shl 2 or message.orientation)
        return builder.toGamePacket()
    }
}