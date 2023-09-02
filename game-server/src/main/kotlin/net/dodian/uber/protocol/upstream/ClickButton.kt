package net.dodian.uber.protocol.upstream

import net.dodian.uber.protocol.packet.DownstreamPacket

data class ClickButton(
    val widgetId: Int
) : DownstreamPacket