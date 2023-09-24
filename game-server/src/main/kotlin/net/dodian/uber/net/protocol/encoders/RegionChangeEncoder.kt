package net.dodian.uber.net.protocol.encoders

import net.dodian.uber.net.codec.game.DataTransformation
import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.RegionChangeMessage

class RegionChangeEncoder : MessageEncoder<RegionChangeMessage>() {

    override fun encode(message: RegionChangeMessage): GamePacket {
        val builder = GamePacketBuilder(73)
        builder.put(DataType.SHORT, DataTransformation.ADD, message.position.centralRegionX)
        builder.put(DataType.SHORT, message.position.centralRegionY)
        return builder.toGamePacket()
    }
}