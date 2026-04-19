package net.dodian.uber.game.engine.systems.combat

import net.dodian.uber.game.model.entity.Entity

data class CombatEngagementState(
    val intent: CombatIntent,
    val targetType: Entity.Type,
    val targetSlot: Int,
    val startedCycle: Long,
    val lastInRangeConfirmationCycle: Long,
    val autoFollowEnabled: Boolean,
    val lastFollowCycle: Long,
    val lastFollowTargetX: Int,
    val lastFollowTargetY: Int,
    val lastFollowTargetDeltaX: Int,
    val lastFollowTargetDeltaY: Int,
)
