package net.dodian.uber.game.runtime.sync.player.root

data class RootPlayerInfoPlan(
    val mode: PlayerPacketMode,
    val buildReason: PlayerPacketBuildReason?,
    val skipReason: PlayerPacketSkipReason?,
    val diff: DesiredLocalSetDiff,
    val desiredLocalSet: DesiredLocalSet,
    val pendingAddCount: Int,
    val actualAdditions: IntArray,
    val deferredAdditionCount: Int,
    val selfMovementChanged: Boolean,
    val selfBlockChanged: Boolean,
    val recoveryReason: PlayerSyncRecoveryReason? = null,
)
