package net.dodian.uber.game.sync.segment

import net.dodian.uber.game.sync.block.SynchronizationBlockSet

interface SynchronizationSegment {
    val blockSet: SynchronizationBlockSet
    val segmentType: SegmentType
}