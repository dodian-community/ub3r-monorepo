package net.dodian.uber.game.runtime.sync.player

data class ViewerPlayerSyncState(
    var lastPlayerSyncTick: Long = 0L,
    var lastSelfMovementRevision: Long = 0L,
    var lastSelfBlockRevision: Long = 0L,
    var lastViewportRevision: Long = 0L,
    var lastKnownLocalCount: Int = 0,
    var lastKnownMapRegionX: Int = Int.MIN_VALUE,
    var lastKnownMapRegionY: Int = Int.MIN_VALUE,
    var lastKnownPlane: Int = Int.MIN_VALUE,
    var lastKnownTeleportState: Boolean = false,
    var lastChunkActivityStamp: Long = 0L,
    var lastLocalActivityStamp: Long = 0L,
)
