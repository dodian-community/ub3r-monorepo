package net.dodian.uber.net.protocol.packets.server

import net.dodian.uber.net.message.Message

data class SetWidgetTextMessage(
    val interfaceId: Int,
    val text: String
) : Message()