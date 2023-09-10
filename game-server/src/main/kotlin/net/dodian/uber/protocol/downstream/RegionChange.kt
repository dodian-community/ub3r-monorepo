package net.dodian.uber.protocol.downstream

import net.dodian.uber.game.model.Position
import net.dodian.uber.protocol.packet.DownstreamPacket



data class RegionChange(val position: Position) : DownstreamPacket