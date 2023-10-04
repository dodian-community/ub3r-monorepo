package net.dodian.uber.net.protocol.encoders.region

import net.dodian.extensions.built
import net.dodian.uber.net.codec.game.DataTransformation
import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.region.SendPublicTileItemMessage

class SendPublicTileItemMessageEncoder : MessageEncoder<SendPublicTileItemMessage>() {
    override fun encode(message: SendPublicTileItemMessage) = GamePacketBuilder(215).apply {
        put(DataType.SHORT, DataTransformation.ADD, message.item.id)
        put(DataType.BYTE, DataTransformation.SUBTRACT, message.offset)
        put(DataType.SHORT, DataTransformation.ADD, message.index)
        put(DataType.SHORT, message.item.amount)
    }.built
}