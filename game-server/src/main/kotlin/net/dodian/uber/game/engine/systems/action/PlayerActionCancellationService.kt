package net.dodian.uber.game.engine.systems.action

import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.engine.systems.skills.asSkillPlayer
import net.dodian.uber.game.skill.cooking.Cooking
import net.dodian.uber.game.skill.fishing.Fishing
import net.dodian.uber.game.skill.fletching.Fletching
import net.dodian.uber.game.skill.crafting.Crafting
import net.dodian.uber.game.skill.prayer.Prayer
import net.dodian.uber.game.skill.smithing.Smithing
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.skill.smithing.SmithingInterface
import net.dodian.uber.game.engine.systems.combat.CombatPreemptionPolicy
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
        Smithing.stopFromReset(player, fullResetAnimation)
        Prayer.stopFromReset(player, fullResetAnimation)
        Crafting.stopFromReset(player, fullResetAnimation)
        Fletching.stopFromReset(player, fullResetAnimation)
        Fishing.stopFromReset(player, fullResetAnimation)
        Cooking.stopFromReset(player, fullResetAnimation)
        WoodcuttingModule.stopAction(player.asSkillPlayer())
        MiningModule.stopAction(player.asSkillPlayer())
        player.clearFletchingState()
        player.contentRuntimeState.setResourcesGathered(0)
        if (player.activeSmithingSelection != null || player.IsAnvil) {
            SmithingInterface.resetRuntimeState(player)
            player.send(RemoveInterfaces())
        }
        player.clearPendingProductionSelection()
        player.clearActiveProductionSelection()
        player.contentRuntimeState.clearPendingSkillMulti()
        player.NpcWanneTalk = 0
        if (fullResetAnimation) {
            player.rerequestAnim()
        }
    }
}
