package net.dodian.uber.game.content.skills.core.runtime

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.skilling.SkillingActionStoppedEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.content.skills.core.events.SkillActionInterruptEvent

object SkillingInterruptService {
    @JvmStatic
    fun stopReason(reason: PlayerActionCancelReason?): ActionStopReason = ActionStopReasonMapper.fromCancelReason(reason)

    @JvmStatic
    fun postStopped(player: Client, actionName: String, reason: PlayerActionCancelReason?) {
        GameEventBus.post(
            SkillingActionStoppedEvent(
                client = player,
                actionName = actionName,
                reason = stopReason(reason),
            ),
        )
        GameEventBus.post(SkillActionInterruptEvent(player, actionName, stopReason(reason)))
    }
}
