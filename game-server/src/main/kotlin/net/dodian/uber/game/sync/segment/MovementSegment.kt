package net.dodian.uber.game.sync.segment

import net.dodian.uber.game.model.Direction
import net.dodian.uber.game.sync.block.SynchronizationBlockSet

class MovementSegment(
    blockSet: SynchronizationBlockSet,
    private val directions: Array<Direction>
) : SynchronizationSegment(blockSet) {

    override val type: SegmentType
        get() = when (directions.size) {
            0 -> SegmentType.NO_MOVEMENT
            1 -> SegmentType.WALK
            2 -> SegmentType.RUN
            else -> error("Direction type '${directions.size}' unsupported.")
        }
}