package net.dodian.uber.net.protocol.packets.server

import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.net.message.Message

data class RegionChangeMessage(
    val position: Position
) : Message()