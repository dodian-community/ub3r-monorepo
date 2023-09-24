package net.dodian.uber.net.protocol.encoders

import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.LogoutMessage

class LogoutEncoder : MessageEncoder<LogoutMessage>() {
    override fun encode(message: LogoutMessage): GamePacket = GamePacketBuilder(109).toGamePacket()
}