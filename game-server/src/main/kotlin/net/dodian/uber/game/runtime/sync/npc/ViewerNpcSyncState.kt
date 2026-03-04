package net.dodian.uber.game.runtime.sync.npc

data class ViewerNpcSyncState(
    var lastNpcSyncTick: Long = 0L,
    var lastNpcViewportRevision: Long = 0L,
    var lastKnownLocalNpcCount: Int = 0,
    var lastKnownMapRegionX: Int = Int.MIN_VALUE,
    var lastKnownMapRegionY: Int = Int.MIN_VALUE,
    var lastKnownPlane: Int = Int.MIN_VALUE,
    var lastChunkActivityStamp: Long = 0L,
    var lastLocalNpcActivityStamp: Long = 0L,
)
