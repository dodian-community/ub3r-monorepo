package net.dodian.uber.net.protocol.encoders

import net.dodian.extensions.built
import net.dodian.uber.net.codec.game.DataOrder
import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.ConfigMessage

class ConfigMessageEncoder : MessageEncoder<ConfigMessage>() {

    override fun encode(message: ConfigMessage) = GamePacketBuilder().apply {
        val value = message.value

        put(DataType.SHORT, DataOrder.LITTLE, message.id)

        if (value > Byte.MIN_VALUE && value < Byte.MAX_VALUE) {
            opcode = 36

            put(DataType.BYTE, value and 0xFF)
        } else {
            opcode = 87

            put(DataType.INT, DataOrder.MIDDLE, value)
        }
    }.built
}