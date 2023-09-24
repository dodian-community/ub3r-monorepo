package net.dodian.uber.net.protocol.packets.client

import net.dodian.uber.net.message.Message

class PublicChatMessage(
    message: String,
    compressedMessage: ByteArray,
    val color: Int,
    val effects: Int,
) : ChatMessage(message, compressedMessage)