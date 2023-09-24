package net.dodian.uber.game.sync.segment

import net.dodian.uber.game.modelkt.area.Direction
import net.dodian.uber.game.sync.block.SynchronizationBlockSet

class MovementSegment(
    val directions: Array<Direction>,
    override val blockSet: SynchronizationBlockSet
) : SynchronizationSegment {

    init {
        if (directions.size !in 0..2)
            error("Directions length must be between 0 and 2 (inclusive).")
    }

    override val segmentType: SegmentType
        get() = when (directions.size) {
            0 -> SegmentType.NO_MOVEMENT
            1 -> SegmentType.WALK
            2 -> SegmentType.RUN
            else -> error("Direction type unsupported.")
        }
}