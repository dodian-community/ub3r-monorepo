package net.dodian.uber.net.protocol.decoders

import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketReader
import net.dodian.uber.net.message.MessageDecoder
import net.dodian.uber.net.protocol.packets.client.MouseClickedMessage

class MouseClickedMessageDecoder : MessageDecoder<MouseClickedMessage>() {

    override fun decode(packet: GamePacket): MouseClickedMessage {
        val reader = GamePacketReader(packet)
        val value = reader.getUnsigned(DataType.INT).toInt()

        val delay: Long = ((value shr 20) * 50).toLong()
        val right: Boolean = (value shr 19 and 0x1) == 1

        val coords = value and 0x3FFFF
        val x = coords % 765
        val y = coords / 765

        return MouseClickedMessage(delay, right, x, y)
    }
}