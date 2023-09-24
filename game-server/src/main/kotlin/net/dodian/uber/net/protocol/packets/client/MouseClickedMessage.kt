package net.dodian.uber.net.protocol.packets.client

import net.dodian.uber.net.message.Message

class MouseClickedMessage(
    val clickDelay: Long,
    val rightClick: Boolean,
    val x: Int,
    val y: Int
) : Message()