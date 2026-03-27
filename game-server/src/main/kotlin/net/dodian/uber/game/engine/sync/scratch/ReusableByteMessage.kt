package net.dodian.uber.game.engine.sync.scratch

import net.dodian.uber.game.netty.codec.ByteMessage

class ReusableByteMessage(private val backing: ByteMessage) {
    fun acquire(): ByteMessage = backing.clear()
}
