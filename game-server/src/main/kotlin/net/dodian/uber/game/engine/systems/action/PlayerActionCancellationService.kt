package net.dodian.uber.game.engine.systems.action

import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.engine.systems.skills.asSkillPlayer
import net.dodian.uber.game.skill.runtime.action.ActionStopReason
import net.dodian.uber.skills.cooking.CookingModule
import net.dodian.uber.skills.fishing.FishingModule
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.engine.systems.skills.SmithingAnvilBridge
import net.dodian.uber.game.engine.systems.combat.CombatPreemptionPolicy
import net.dodian.uber.game.engine.systems.skills.SmithingSmeltingBridge
import net.dodian.uber.skills.mining.MiningModule
import net.dodian.uber.skills.woodcutting.WoodcuttingModule

object PlayerActionCancellationService {
    @JvmStatic
    @JvmOverloads
    @Deprecated(
        message = "Use ContentActions.cancel for content-facing calls.",
        replaceWith = ReplaceWith(
            expression = "ContentActions.cancel(player, reason, fullResetAnimation, clearDialogue, closeInterfaces, resetCompatibilityState)",
            imports = arrayOf("net.dodian.uber.game.api.content.ContentActions"),
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
        SmithingSmeltingBridge.clearSelection(player)
        FishingModule.stopAction(player.asSkillPlayer(), ActionStopReason.USER_INTERRUPT)
        CookingModule.stopAction(player.asSkillPlayer(), ActionStopReason.USER_INTERRUPT)
        WoodcuttingModule.stopAction(player.asSkillPlayer())
        MiningModule.stopAction(player.asSkillPlayer())
        player.contentRuntimeState.setResourcesGathered(0)
        SmithingAnvilBridge.resetRuntimeState(player)
        player.clearPendingProductionSelection()
        player.clearActiveProductionSelection()
        player.contentRuntimeState.clearPendingSkillMulti()
        player.NpcWanneTalk = 0
        if (fullResetAnimation) {
            player.rerequestAnim()
        }
    }
}
