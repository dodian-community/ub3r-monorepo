package net.dodian.uber.net.protocol.packets.server

import net.dodian.uber.net.message.Message

data class ConfigMessage(
    val id: Int,
    val value: Int
) : Message()