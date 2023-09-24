package net.dodian.uber.net.protocol.encoders.region

import net.dodian.extensions.built
import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.region.UpdateTileItemMessage

class UpdateTileItemMessageEncoder : MessageEncoder<UpdateTileItemMessage>() {

    override fun encode(message: UpdateTileItemMessage) = GamePacketBuilder(121).apply {
        put(DataType.BYTE, message.offset)
        put(DataType.SHORT, message.item.id)
        put(DataType.SHORT, message.previousAmount)
        put(DataType.SHORT, message.item.amount)
    }.built
}