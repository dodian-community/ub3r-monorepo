package net.dodian.uber.game.persistence.player

data class PlayerSaveSegmentSnapshot(
    val segment: PlayerSaveSegment,
    val estimatedEntries: Int = 0,
)
