package net.dodian.uber.net.protocol.encoders.region

import net.dodian.uber.net.codec.game.DataTransformation
import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.region.RemoveTileItemMessage

class RemoveTileItemMessageEncoder : MessageEncoder<RemoveTileItemMessage>() {

    override fun encode(message: RemoveTileItemMessage): GamePacket {
        val builder = GamePacketBuilder(156)
        builder.put(DataType.BYTE, DataTransformation.ADD, message.offset)
        builder.put(DataType.SHORT, message.id)
        return builder.toGamePacket()
    }
}