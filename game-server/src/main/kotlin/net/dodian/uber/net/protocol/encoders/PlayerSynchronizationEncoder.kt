package net.dodian.uber.net.protocol.encoders

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.net.codec.game.DataType
import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.message.meta.PacketType
import net.dodian.uber.net.protocol.packets.server.PlayerSynchronization

private val logger = InlineLogger()

class PlayerSynchronizationEncoder : MessageEncoder<PlayerSynchronization>() {

    override fun encode(message: PlayerSynchronization): GamePacket {
        val builder = GamePacketBuilder(81, PacketType.VARIABLE_SHORT)

        builder.switchToBitAccess()
        builder.putBits(1, 1) // Player updated
        builder.putBits(2, 3) // Teleported
        builder.putBits(2, 0) // Height Level
        builder.putBits(1, 1) // Should the walk queue be discarded?
        builder.putBits(1, 1) // Is there a block update?
        builder.putBits(7, 66) // Local Y coordinate
        builder.putBits(7, 66) // Local X coordinate
        builder.putBits(8, 0) // Number of local players
        builder.putBits(11, 2047) // Magic id to indicate blocks follow
        builder.switchToByteAccess()
        builder.put(DataType.BYTE, 0)

        return builder.toGamePacket()
    }
}