package net.dodian.uber.protocol.downstream

import net.dodian.uber.game.model.Position
import net.dodian.uber.protocol.packet.DownstreamPacket

data class RegionUpdate(
    val player: Position,
    val region: Position
) : DownstreamPacket