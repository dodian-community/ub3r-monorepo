package net.dodian.uber.net.protocol.encoders

import net.dodian.extensions.built
import net.dodian.uber.net.codec.game.DataOrder
import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.OpenOverlayMessage

class OpenOverlayMessageEncoder : MessageEncoder<OpenOverlayMessage>() {

    override fun encode(message: OpenOverlayMessage) = GamePacketBuilder(208).apply {
        put(DataType.SHORT, DataOrder.LITTLE, message.overlayId)
    }.built
}