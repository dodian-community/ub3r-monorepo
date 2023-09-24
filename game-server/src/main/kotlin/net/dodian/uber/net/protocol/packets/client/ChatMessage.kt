package net.dodian.uber.net.protocol.packets.client

import net.dodian.uber.net.message.Message

abstract class ChatMessage(val message: String, messageCompressed: ByteArray) : Message() {
    val messageCompressed: ByteArray

    init {
        this.messageCompressed = messageCompressed.clone()
    }
}