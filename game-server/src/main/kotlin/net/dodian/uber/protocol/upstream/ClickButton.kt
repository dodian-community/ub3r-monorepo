package net.dodian.uber.protocol.upstream

import net.dodian.uber.protocol.packet.UpstreamPacket

data class ClickButton(
    val widgetId: Int
) : UpstreamPacket