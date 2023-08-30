package net.dodian.uber.net.release

import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.message.Message

abstract class MessageDecoder<M : Message> {
    abstract fun decode(packet: GamePacket): M
}