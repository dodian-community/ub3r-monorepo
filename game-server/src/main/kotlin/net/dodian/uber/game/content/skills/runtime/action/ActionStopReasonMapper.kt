package net.dodian.uber.game.content.skills.runtime.action

import net.dodian.uber.game.engine.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.engine.systems.action.PlayerActionStopResult

object ActionStopReasonMapper {
    @JvmStatic
    fun fromCancelReason(reason: PlayerActionCancelReason?): ActionStopReason =
        when (reason) {
            null -> ActionStopReason.COMPLETED
            PlayerActionCancelReason.NEW_ACTION,
            PlayerActionCancelReason.MANUAL_RESET,
            PlayerActionCancelReason.ITEM_INTERACTION,
            PlayerActionCancelReason.OBJECT_INTERACTION,
            PlayerActionCancelReason.NPC_INTERACTION,
            PlayerActionCancelReason.PLAYER_INTERACTION,
            PlayerActionCancelReason.GROUND_ITEM_INTERACTION,
            -> ActionStopReason.USER_INTERRUPT
            PlayerActionCancelReason.MOVEMENT -> ActionStopReason.MOVED_AWAY
            PlayerActionCancelReason.INTERFACE_CLOSED -> ActionStopReason.USER_INTERRUPT
            PlayerActionCancelReason.DIALOGUE_OPENED -> ActionStopReason.BUSY
            PlayerActionCancelReason.LOGOUT,
            PlayerActionCancelReason.DISCONNECTED,
            -> ActionStopReason.DISCONNECTED
            PlayerActionCancelReason.DEATH,
            PlayerActionCancelReason.TELEPORT,
            PlayerActionCancelReason.COMBAT_INTERRUPTED,
            -> ActionStopReason.INVALID_TARGET
        }

    @JvmStatic
    fun fromStopResult(result: PlayerActionStopResult): ActionStopReason =
        when (result) {
            PlayerActionStopResult.Completed -> ActionStopReason.COMPLETED
            is PlayerActionStopResult.Cancelled -> fromCancelReason(result.reason)
        }
}
