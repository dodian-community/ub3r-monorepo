package net.dodian.uber.net.protocol.decoders

import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.message.MessageDecoder
import net.dodian.uber.net.protocol.packets.client.KeepAlive

class KeepAliveDecoder : MessageDecoder<KeepAlive>() {

    override fun decode(packet: GamePacket): KeepAlive {
        return KeepAlive()
    }
}