package net.dodian.uber.game.skills.core

import net.dodian.uber.game.event.GameEventBus
import net.dodian.uber.game.event.events.skilling.SkillingActionStoppedEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason

object SkillingInterruptService {
    @JvmStatic
    fun stopReason(reason: PlayerActionCancelReason?): ActionStopReason =
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
    fun postStopped(player: Client, actionName: String, reason: PlayerActionCancelReason?) {
        GameEventBus.post(
            SkillingActionStoppedEvent(
                client = player,
                actionName = actionName,
                reason = stopReason(reason),
            ),
        )
    }
}
