package net.dodian.uber.net.message

import net.dodian.uber.net.codec.game.GamePacket

abstract class MessageEncoder<M : Message> {
    abstract fun encode(message: M): GamePacket
}