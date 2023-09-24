package net.dodian.uber.game.sync.segment

import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.game.sync.block.SynchronizationBlockSet

data class TeleportSegment(
    val destination: Position,
    override val blockSet: SynchronizationBlockSet,
    override val segmentType: SegmentType = SegmentType.TELEPORT
) : SynchronizationSegment