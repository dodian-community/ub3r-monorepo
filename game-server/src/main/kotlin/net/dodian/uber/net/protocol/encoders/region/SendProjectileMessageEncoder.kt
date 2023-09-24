package net.dodian.uber.net.protocol.encoders.region

import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.region.SendProjectileMessage

class SendProjectileMessageEncoder : MessageEncoder<SendProjectileMessage>() {

    override fun encode(message: SendProjectileMessage): GamePacket {
        val projectile = message.projectile
        val source = projectile.position
        val destination = projectile.destination

        return GamePacketBuilder(181).apply {
            put(DataType.BYTE, message.offset)
            put(DataType.BYTE, destination.x - source.x)
            put(DataType.BYTE, destination.y - source.y)
            put(DataType.SHORT, projectile.target)
            put(DataType.SHORT, projectile.graphic)
            put(DataType.BYTE, projectile.startHeight)
            put(DataType.BYTE, projectile.endHeight)
            put(DataType.SHORT, projectile.delay)
            put(DataType.SHORT, projectile.lifetime)
            put(DataType.BYTE, projectile.pitch)
            put(DataType.BYTE, projectile.offset)
        }.toGamePacket()
    }
}