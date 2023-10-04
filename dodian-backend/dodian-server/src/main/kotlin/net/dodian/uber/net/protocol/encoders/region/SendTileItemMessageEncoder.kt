package net.dodian.uber.net.protocol.encoders.region

import net.dodian.extensions.built
import net.dodian.uber.net.codec.game.*
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.region.SendTileItemMessage

class SendTileItemMessageEncoder : MessageEncoder<SendTileItemMessage>() {
    override fun encode(message: SendTileItemMessage) = GamePacketBuilder(44).apply {
        put(DataType.SHORT, DataOrder.LITTLE, DataTransformation.ADD, message.item.id)
        put(DataType.SHORT, message.item.amount)
        put(DataType.BYTE, message.offset)
    }.built
}