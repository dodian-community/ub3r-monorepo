package net.dodian.uber.net.message

import net.dodian.uber.net.codec.game.GamePacket

abstract class MessageDecoder<M : Message> {
    abstract fun decode(packet: GamePacket): M
}