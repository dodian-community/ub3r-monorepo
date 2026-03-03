package net.dodian.uber.game.runtime.sync.player.root

data class ViewerDesiredLocalState(
    var currentLocalSlots: IntArray = IntArray(255),
    var currentLocalCount: Int = 0,
    var desiredLocalSlots: IntArray = IntArray(255),
    var desiredLocalCount: Int = 0,
    var pendingAddSlots: IntArray = IntArray(16),
    var pendingAddCount: Int = 0,
    var pendingAddHead: Int = 0,
    var pendingAddTail: Int = 0,
    var pendingAddSignature: Int = 0,
    var lastVisibleSignature: Int = 0,
    var lastRegionBaseX: Int = Int.MIN_VALUE,
    var lastRegionBaseY: Int = Int.MIN_VALUE,
    var lastPlane: Int = Int.MIN_VALUE,
    var needsHardRebuild: Boolean = true,
    var lastPacketMode: PlayerPacketMode = PlayerPacketMode.FULL_REBUILD,
)
