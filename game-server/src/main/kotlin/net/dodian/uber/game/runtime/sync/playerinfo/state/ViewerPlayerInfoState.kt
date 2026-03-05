package net.dodian.uber.game.runtime.sync.playerinfo.state

import net.dodian.uber.game.runtime.sync.playerinfo.dispatch.PlayerPacketMode

data class ViewerPlayerInfoState(
    var lastKnownLocalSlots: IntArray = IntArray(0),
    var lastKnownLocalCount: Int = 0,
    var lastKnownRegionBaseX: Int = Int.MIN_VALUE,
    var lastKnownRegionBaseY: Int = Int.MIN_VALUE,
    var lastKnownPlane: Int = Int.MIN_VALUE,
    var lastBuildAreaSignature: Int = 0,
    var lastVisibleSignature: Int = 0,
    var lastLocalActivityStamp: Long = 0L,
    var lastPacketMode: PlayerPacketMode = PlayerPacketMode.FULL_REBUILD,
    var needsHardRebuild: Boolean = true,
    val buildAreaState: PlayerBuildAreaState = PlayerBuildAreaState(),
    val desiredLocalState: ViewerDesiredLocalState = ViewerDesiredLocalState(),
)
