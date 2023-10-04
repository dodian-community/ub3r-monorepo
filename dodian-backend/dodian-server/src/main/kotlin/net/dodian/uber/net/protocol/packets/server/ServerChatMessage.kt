package net.dodian.uber.net.protocol.packets.server

import net.dodian.uber.net.message.Message

data class ServerChatMessage(
    val message: String
) : Message()