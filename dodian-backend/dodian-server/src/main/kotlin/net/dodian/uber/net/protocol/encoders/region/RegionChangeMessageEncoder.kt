package net.dodian.uber.net.protocol.encoders.region

import net.dodian.extensions.built
import net.dodian.uber.net.codec.game.DataTransformation
import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.region.RegionChangeMessage

class RegionChangeMessageEncoder : MessageEncoder<RegionChangeMessage>() {

    override fun encode(message: RegionChangeMessage) = GamePacketBuilder(73).apply {
        put(DataType.SHORT, DataTransformation.ADD, message.position.centralRegionX)
        put(DataType.SHORT, message.position.centralRegionY)
    }.built
}