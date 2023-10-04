package net.dodian.uber.net.protocol.encoders

import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.message.meta.PacketType
import net.dodian.uber.net.protocol.packets.server.ServerChatMessage

class ServerChatEncoder : MessageEncoder<ServerChatMessage>() {

    override fun encode(message: ServerChatMessage): GamePacket {
        val builder = GamePacketBuilder(253, PacketType.VARIABLE_BYTE)
        builder.putString(message.message)
        return builder.toGamePacket()
    }
}