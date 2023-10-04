package net.dodian.uber.net.protocol.encoders.region

import net.dodian.uber.net.codec.game.DataTransformation
import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.region.RemoveObjectMessage

class RemoveObjectMessageEncoder : MessageEncoder<RemoveObjectMessage>() {

    override fun encode(message: RemoveObjectMessage): GamePacket {
        val builder = GamePacketBuilder(101)
        builder.put(DataType.BYTE, DataTransformation.NEGATE, message.type shl 2 or message.orientation)
        builder.put(DataType.BYTE, message.offset)
        return builder.toGamePacket()
    }
}