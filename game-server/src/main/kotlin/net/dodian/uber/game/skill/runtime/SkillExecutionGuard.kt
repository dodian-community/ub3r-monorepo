package net.dodian.uber.game.skill.runtime

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.follow.FollowService
import net.dodian.uber.game.model.entity.player.MovementLockState
import net.dodian.uber.game.skill.runtime.action.SkillStateCoordinator
import net.dodian.uber.game.engine.loop.GameCycleClock

data class SkillExecutionGuard(
    val lockMovement: Boolean = true,
    val lockFollow: Boolean = true,
    val cancelFollowOnStart: Boolean = true,
    val sessionKey: String = "skill:movement_lock",
    val isBusy: (Client) -> Boolean = { it.isMovementLocked },
) {
    fun tryBegin(context: SkillActionContext): Boolean {
        val player = context.player
        if (isBusy(player)) {
            return false
        }
        if (!SkillStateCoordinator.beginSession(player, sessionKey)) {
            return false
        }
        if (lockFollow && cancelFollowOnStart) {
            FollowService.cancelFollow(player)
        }
        if (lockMovement) {
            player.setMovementLockState(MovementLockState(sessionKey, GameCycleClock.currentCycle()))
        }
        return true
    }

    fun finish(context: SkillActionContext) {
        if (lockMovement) {
            context.player.clearMovementLockState()
        }
        SkillStateCoordinator.endSession(context.player, sessionKey)
    }

    companion object {
        @JvmStatic
        fun agilityDefault(): SkillExecutionGuard = SkillExecutionGuard()
    }
}
