package net.dodian.uber.game.content.skills.core.runtime

import net.dodian.uber.game.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.systems.action.PlayerActionStopResult

object ActionStopReasonMapper {
    @JvmStatic
    fun fromCancelReason(reason: PlayerActionCancelReason?): ActionStopReason =
        net.dodian.uber.game.systems.skills.ActionStopReasonMapper.fromCancelReason(reason)

    @JvmStatic
    fun fromStopResult(result: PlayerActionStopResult): ActionStopReason =
        net.dodian.uber.game.systems.skills.ActionStopReasonMapper.fromStopResult(result)
}
