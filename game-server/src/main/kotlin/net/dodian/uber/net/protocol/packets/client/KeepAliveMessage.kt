package net.dodian.uber.net.protocol.packets.client

import net.dodian.uber.net.message.Message

data class KeepAliveMessage(
    val createdAt: Long = System.currentTimeMillis()
) : Message()