package net.dodian.uber.net.protocol.encoders

import net.dodian.uber.net.codec.game.DataTransformation
import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.SwitchTabInterfaceMessage

class SwitchTabInterfaceMessageEncoder : MessageEncoder<SwitchTabInterfaceMessage>() {

    override fun encode(message: SwitchTabInterfaceMessage): GamePacket {
        val builder = GamePacketBuilder(71)
        builder.put(DataType.SHORT, message.interfaceId)
        builder.put(DataType.BYTE, DataTransformation.ADD, message.tab)
        return builder.toGamePacket()
    }
}