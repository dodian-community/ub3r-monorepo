package net.dodian.uber.game.systems.combat

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.action.PlayerActionCancelReason

object CombatPreemptionPolicy {
    @JvmStatic
    fun preemptCombatIfNeeded(player: Client, reason: PlayerActionCancelReason) {
        val mappedReason = mapCancelReason(reason) ?: return
        if (!CombatRuntimeService.hasActiveCombat(player)) {
            return
        }
        CombatRuntimeService.cancel(player, mappedReason)
    }

    private fun mapCancelReason(reason: PlayerActionCancelReason): CombatCancellationReason? =
        when (reason) {
            PlayerActionCancelReason.MOVEMENT -> CombatCancellationReason.MOVEMENT_INTERRUPTED
            PlayerActionCancelReason.OBJECT_INTERACTION,
            PlayerActionCancelReason.GROUND_ITEM_INTERACTION,
            PlayerActionCancelReason.ITEM_INTERACTION,
            PlayerActionCancelReason.NPC_INTERACTION,
            PlayerActionCancelReason.DIALOGUE_OPENED,
            PlayerActionCancelReason.INTERFACE_CLOSED,
            PlayerActionCancelReason.MANUAL_RESET,
            -> CombatCancellationReason.INTERACTION_PREEMPTED
            else -> null
        }
}
