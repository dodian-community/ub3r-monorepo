package net.dodian.uber.game.systems.action

import net.dodian.uber.game.systems.ui.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.content.skills.smithing.SmithingInterface
import net.dodian.uber.game.systems.combat.CombatPreemptionPolicy

object PlayerActionCancellationService {
    @JvmStatic
    @JvmOverloads
    @Deprecated(
        message = "Use ContentActions.cancel for content-facing calls.",
        replaceWith = ReplaceWith(
            expression = "ContentActions.cancel(player, reason, fullResetAnimation, clearDialogue, closeInterfaces, resetCompatibilityState)",
            imports = arrayOf("net.dodian.uber.game.systems.api.content.ContentActions"),
        ),
    )
    fun cancel(
        player: Client,
        reason: PlayerActionCancelReason,
        fullResetAnimation: Boolean = true,
        clearDialogue: Boolean = false,
        closeInterfaces: Boolean = false,
        resetCompatibilityState: Boolean = true,
    ) {
        if (closeInterfaces) {
            player.send(RemoveInterfaces())
        }
        if (clearDialogue) {
            DialogueService.closeBlockingDialogue(player, closeInterfaces = false)
        }
        CombatPreemptionPolicy.preemptCombatIfNeeded(player, reason)
        PlayerActionController.cancel(player, reason)
        if (resetCompatibilityState) {
            resetCompatibilityState(player, fullResetAnimation)
        }
    }

    @JvmStatic
    fun resetCompatibilityState(
        player: Client,
        fullResetAnimation: Boolean,
    ) {
        player.clearSmeltingSelection()
        player.clearPendingSmeltingBarId()
        player.clearPrayerOfferingState()
        player.clearCraftingState()
        SkillingActionService.stopFletchingFromReset(player, fullResetAnimation)
        SkillingActionService.stopFishingFromReset(player, fullResetAnimation)
        SkillingActionService.stopCookingFromReset(player, fullResetAnimation)
        player.clearFletchingState()
        player.clearFishingState()
        player.resourcesGathered = 0
        player.clearCookingState()
        player.clearMiningState()
        player.clearWoodcuttingState()
        if (player.getActiveSmithingSelection() != null || player.IsAnvil) {
            SmithingInterface.resetRuntimeState(player)
            player.send(RemoveInterfaces())
        }
        player.clearPendingProductionSelection()
        player.clearActiveProductionSelection()
        player.NpcWanneTalk = 0
        if (fullResetAnimation) {
            player.rerequestAnim()
        }
    }
}
