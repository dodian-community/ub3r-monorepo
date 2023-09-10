package net.dodian.uber.protocol.downstream

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.sync.segment.SynchronizationSegment
import net.dodian.uber.protocol.packet.DownstreamPacket

data class PlayerSynchronization(
    val lastKnownRegion: Position,
    val localPlayers: Int,
    val position: Position,
    val regionChanged: Boolean,
    val segment: SynchronizationSegment,
    val segments: List<SynchronizationSegment>
) : DownstreamPacket