package net.dodian.uber.net.protocol.encoders

import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.message.meta.PacketType
import net.dodian.uber.net.protocol.packets.server.ServerChat

class ServerChatEncoder : MessageEncoder<ServerChat>() {

    override fun encode(message: ServerChat): GamePacket {
        val builder = GamePacketBuilder(253, PacketType.VARIABLE_BYTE)
        builder.putString(message.message)
        return builder.toGamePacket()
    }
}