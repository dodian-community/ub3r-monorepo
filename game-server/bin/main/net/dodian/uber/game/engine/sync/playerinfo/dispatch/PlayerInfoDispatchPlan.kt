package net.dodian.uber.game.engine.sync.playerinfo.dispatch

import net.dodian.uber.game.engine.sync.playerinfo.state.PlayerInfoLocalSetDiff

data class PlayerInfoDispatchPlan(
    val mode: PlayerPacketMode,
    val buildReason: PlayerPacketBuildReason?,
    val skipReason: PlayerPacketSkipReason?,
    val diff: PlayerInfoLocalSetDiff,
    val visibleSlots: IntArray,
    val visibleCount: Int,
    val selfMovementChanged: Boolean,
    val selfBlockChanged: Boolean,
    val actualAdditions: IntArray,
)
