package net.dodian.uber.game.sync.segment

import net.dodian.uber.game.sync.block.SynchronizationBlockSet

data class RemoveMobSegment(
    override val blockSet: SynchronizationBlockSet = SynchronizationBlockSet(),
    override val segmentType: SegmentType = SegmentType.REMOVE_MOB
) : SynchronizationSegment