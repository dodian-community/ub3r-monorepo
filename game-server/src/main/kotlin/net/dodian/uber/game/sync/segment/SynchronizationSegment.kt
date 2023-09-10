package net.dodian.uber.game.sync.segment

import net.dodian.uber.game.sync.block.SynchronizationBlockSet

abstract class SynchronizationSegment(
    val blockSet: SynchronizationBlockSet
) {
    abstract val type: SegmentType
}