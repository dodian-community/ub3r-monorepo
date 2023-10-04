package net.dodian.uber.net.protocol.packets.client

import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.net.message.Message

data class WalkMessage(
    val steps: List<Position>,
    val isRunning: Boolean
) : Message()