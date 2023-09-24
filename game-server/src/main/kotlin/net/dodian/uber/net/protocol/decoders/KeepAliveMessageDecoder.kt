package net.dodian.uber.net.protocol.decoders

import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.message.MessageDecoder
import net.dodian.uber.net.protocol.packets.client.KeepAliveMessage

class KeepAliveMessageDecoder : MessageDecoder<KeepAliveMessage>() {

    override fun decode(packet: GamePacket): KeepAliveMessage {
        return KeepAliveMessage()
    }
}