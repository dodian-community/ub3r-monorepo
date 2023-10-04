package net.dodian.uber.net.protocol.encoders

import net.dodian.extensions.built
import net.dodian.uber.net.codec.game.DataTransformation
import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.message.meta.PacketType
import net.dodian.uber.net.protocol.packets.server.SetWidgetTextMessage

class SetWidgetTextMessageEncoder : MessageEncoder<SetWidgetTextMessage>() {
    override fun encode(message: SetWidgetTextMessage) = GamePacketBuilder(126, PacketType.VARIABLE_SHORT).apply {
        putString(message.text)
        put(DataType.SHORT, DataTransformation.ADD, message.interfaceId)
    }.built
}