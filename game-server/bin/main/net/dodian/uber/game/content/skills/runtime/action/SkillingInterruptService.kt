package net.dodian.uber.game.content.skills.runtime.action

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.skilling.SkillActionInterruptEvent
import net.dodian.uber.game.events.skilling.SkillingActionStoppedEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.action.PlayerActionCancelReason

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
