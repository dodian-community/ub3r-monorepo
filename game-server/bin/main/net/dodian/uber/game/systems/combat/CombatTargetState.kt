package net.dodian.uber.game.systems.combat

import net.dodian.uber.game.model.entity.Entity

data class CombatTargetState(
    val intent: CombatIntent,
    val targetType: Entity.Type,
    val targetSlot: Int,
    val startedCycle: Long,
    val lastInRangeConfirmationCycle: Long,
    val attackStyleAtStart: Int,
    val initialSwingConsumed: Boolean,
    val nextAttackCycle: Long,
    val lastAttackCycle: Long,
    val autoFollowEnabled: Boolean,
    val lastFollowCycle: Long,
    val lastFollowTargetX: Int,
    val lastFollowTargetY: Int,
    val lastFollowTargetDeltaX: Int,
    val lastFollowTargetDeltaY: Int,
)
