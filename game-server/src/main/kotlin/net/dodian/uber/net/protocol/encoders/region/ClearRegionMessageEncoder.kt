package net.dodian.uber.net.protocol.encoders.region

import net.dodian.uber.net.codec.game.DataTransformation
import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.region.ClearRegionMessage

class ClearRegionMessageEncoder : MessageEncoder<ClearRegionMessage>() {

    override fun encode(message: ClearRegionMessage): GamePacket {
        val builder = GamePacketBuilder(64)
        val player = message.player
        val region = message.region

        builder.put(DataType.BYTE, DataTransformation.NEGATE, region.localX(player))
        builder.put(DataType.BYTE, DataTransformation.SUBTRACT, region.localY(player))

        return builder.toGamePacket()
    }
}