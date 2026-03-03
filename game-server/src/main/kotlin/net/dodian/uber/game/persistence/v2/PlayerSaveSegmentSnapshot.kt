package net.dodian.uber.game.persistence.v2

data class PlayerSaveSegmentSnapshot(
    val segment: PlayerSaveSegment,
    val estimatedEntries: Int = 0,
)
