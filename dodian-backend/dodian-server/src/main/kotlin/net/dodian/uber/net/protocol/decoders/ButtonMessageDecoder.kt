package net.dodian.uber.net.protocol.decoders

import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketReader
import net.dodian.uber.net.message.MessageDecoder
import net.dodian.uber.net.protocol.packets.client.ButtonMessage

class ButtonMessageDecoder : MessageDecoder<ButtonMessage>() {

    override fun decode(packet: GamePacket) = ButtonMessage(
        GamePacketReader(packet)
            .getUnsigned(DataType.SHORT)
            .toInt()
    )
}