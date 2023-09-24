package net.dodian.uber.net.protocol.encoders

import net.dodian.protocol
import net.dodian.uber.net.codec.game.DataTransformation
import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.message.meta.PacketType
import net.dodian.uber.net.protocol.packets.server.GroupedRegionUpdateMessage
import net.dodian.uber.net.protocol.packets.server.regionupdate.RegionUpdateMessage

class GroupedRegionUpdateEncoder : MessageEncoder<GroupedRegionUpdateMessage>() {

    @Suppress("UNCHECKED_CAST")
    override fun encode(message: GroupedRegionUpdateMessage): GamePacket {
        val builder = GamePacketBuilder(60, PacketType.VARIABLE_SHORT)
        val base = message.lastKnownRegion
        val region = message.region

        builder.put(DataType.BYTE, region.localY(base))
        builder.put(DataType.BYTE, DataTransformation.NEGATE, region.localX(base))

        message.messages.forEach { update ->
            val encoder = (protocol.encoders[update::class] as MessageEncoder<RegionUpdateMessage>?)
                ?: error("No encoder found for: ${update::class.simpleName}")

            val packet = encoder.encode(update)
            builder.put(DataType.BYTE, packet.opcode)
            builder.putBytes(packet.payload)
        }

        return builder.toGamePacket()
    }
}