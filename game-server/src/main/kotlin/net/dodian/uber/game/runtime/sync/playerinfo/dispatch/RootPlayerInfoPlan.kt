package net.dodian.uber.game.runtime.sync.playerinfo.dispatch

import net.dodian.uber.game.runtime.sync.playerinfo.admission.DesiredLocalSet
import net.dodian.uber.game.runtime.sync.playerinfo.admission.DesiredLocalSetDiff

data class RootPlayerInfoPlan(
    val mode: PlayerPacketMode,
    val buildReason: PlayerPacketBuildReason?,
    val skipReason: PlayerPacketSkipReason?,
    val visibleSignature: Int,
    val diff: DesiredLocalSetDiff,
    val desiredLocalSet: DesiredLocalSet,
    val pendingAddCount: Int,
    val actualAdditions: IntArray,
    val deferredAdditionCount: Int,
    val selfMovementChanged: Boolean,
    val selfBlockChanged: Boolean,
    val recoveryReason: PlayerSyncRecoveryReason? = null,
)
