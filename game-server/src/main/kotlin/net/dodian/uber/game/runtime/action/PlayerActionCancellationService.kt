package net.dodian.uber.game.runtime.action

import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces

object PlayerActionCancellationService {
    @JvmStatic
    @JvmOverloads
    fun cancel(
        player: Client,
        reason: PlayerActionCancelReason,
        fullResetAnimation: Boolean = true,
        clearDialogue: Boolean = false,
        closeInterfaces: Boolean = false,
        resetLegacyState: Boolean = true,
    ) {
        if (closeInterfaces) {
            player.send(RemoveInterfaces())
        }
        if (clearDialogue) {
            DialogueService.closeBlockingDialogue(player, closeInterfaces = false)
        }
        PlayerActionController.cancel(player, reason)
        if (resetLegacyState) {
            resetLegacyState(player, fullResetAnimation)
        }
    }

    @JvmStatic
    fun resetLegacyState(
        player: Client,
        fullResetAnimation: Boolean,
    ) {
        player.smelting = false
        player.smelt_id = -1
        player.goldCrafting = false
        player.goldIndex = -1
        player.goldSlot = -1
        player.boneItem = -1
        player.shafting = false
        player.fletchings = false
        player.spinning = false
        player.setCrafting(false)
        player.setFishing(false)
        player.stringing = false
        player.resourcesGathered = 0
        player.cooking = false
        player.filling = false
        player.clearMiningState()
        player.clearWoodcuttingState()
        if (player.IsAnvil) {
            player.resetSM()
            player.send(RemoveInterfaces())
        }
        player.skillActionTimer = -1
        player.skillActionCount = 0
        player.skillMessage = ""
        if (!player.playerSkillAction.isEmpty()) {
            player.playerSkillAction.clear()
        }
        player.prayerAction = -1
        player.boneItem = -1
        player.NpcWanneTalk = 0
        if (fullResetAnimation) {
            player.rerequestAnim()
        }
    }
}
