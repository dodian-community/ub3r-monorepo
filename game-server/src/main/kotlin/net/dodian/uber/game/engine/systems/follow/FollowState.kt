package net.dodian.uber.game.engine.systems.follow

data class FollowState(
    val followerSlot: Int,
    val followerLongName: Long,
    val targetSlot: Int,
    val targetLongName: Long,
    val startedCycle: Long,
    val firstTickHandled: Boolean,
    val lastTargetX: Int,
    val lastTargetY: Int,
    val lastTargetDeltaX: Int,
    val lastTargetDeltaY: Int,
)
