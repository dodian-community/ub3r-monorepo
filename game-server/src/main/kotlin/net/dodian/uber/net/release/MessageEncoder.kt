package net.dodian.uber.net.release

import net.dodian.uber.net.codec.game.GamePacket
import net.dodian.uber.net.message.Message

abstract class MessageEncoder<M : Message> {
    abstract fun encode(message: M): GamePacket
}