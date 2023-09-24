package net.dodian.uber.net.protocol.packets.client

import net.dodian.uber.net.message.Message

data class ButtonMessage(
    val widgetId: Int
) : Message()