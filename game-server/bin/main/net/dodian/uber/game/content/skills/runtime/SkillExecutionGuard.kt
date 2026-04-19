package net.dodian.uber.game.content.skills.runtime

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.follow.FollowService

data class SkillExecutionGuard(
    val lockMovement: Boolean = true,
    val lockFollow: Boolean = true,
    val cancelFollowOnStart: Boolean = true,
    val isBusy: (Client) -> Boolean = { it.UsingAgility },
) {
    fun tryBegin(context: SkillActionContext): Boolean {
        val player = context.player
        if (isBusy(player)) {
            return false
        }
        if (lockFollow && cancelFollowOnStart) {
            FollowService.cancelFollow(player)
        }
        if (lockMovement) {
            player.UsingAgility = true
        }
        return true
    }

    fun finish(context: SkillActionContext) {
        if (lockMovement) {
            context.player.UsingAgility = false
        }
    }

    companion object {
        @JvmStatic
        fun agilityDefault(): SkillExecutionGuard = SkillExecutionGuard()
    }
}
