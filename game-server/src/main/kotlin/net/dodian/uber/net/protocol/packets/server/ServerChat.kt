package net.dodian.uber.net.protocol.packets.server

import net.dodian.uber.net.message.Message

data class ServerChat(
    val message: String
) : Message()