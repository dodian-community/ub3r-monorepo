package net.dodian.uber.net.protocol.encoders

import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.codec.game.GamePacketBuilder
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.protocol.packets.server.Logout

class LogoutEncoder : MessageEncoder<Logout>() {
    override fun encode(message: Logout): GamePacket = GamePacketBuilder(109).toGamePacket()
}