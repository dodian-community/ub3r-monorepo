package net.dodian.uber.net.protocol.packets.server

import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.game.sync.segment.SynchronizationSegment
import net.dodian.uber.net.message.Message

data class PlayerSynchronizationMessage(
    val lastKnownRegion: Position,
    val localPlayers: Int,
    val position: Position,
    val hasRegionChanged: Boolean,
    val segment: SynchronizationSegment,
    val segments: List<SynchronizationSegment>
) : Message()