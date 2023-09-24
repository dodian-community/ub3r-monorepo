package net.dodian.uber.net.protocol.packets.server

import net.dodian.uber.net.message.Message

data class OpenOverlayMessage(
    val overlayId: Int
) : Message()