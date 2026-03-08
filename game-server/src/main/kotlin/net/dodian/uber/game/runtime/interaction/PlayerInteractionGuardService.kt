package net.dodian.uber.game.runtime.interaction

import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client

object PlayerInteractionGuardService {

    @JvmStatic
    fun hasBlockingInterface(player: Client): Boolean {
        return player.activeInterfaceId != -1 ||
            player.checkBankInterface ||
            player.bankStyleViewOpen ||
            player.IsBanking ||
            player.isPartyInterface ||
            player.isShopping
    }

    @JvmStatic
    fun hasActiveDialogue(player: Client): Boolean = DialogueService.hasBlockingDialogue(player)

    @JvmStatic
    fun canStartTrade(player: Client): Boolean = !hasBlockingInterface(player) && !hasActiveDialogue(player)

    @JvmStatic
    fun canStartDuel(player: Client): Boolean = !hasBlockingInterface(player) && !hasActiveDialogue(player)

    @JvmStatic
    fun canStartSocialRequest(player: Client): Boolean = !hasBlockingInterface(player) && !hasActiveDialogue(player)

    @JvmStatic
    fun tradeBlockMessage(requester: Client, other: Client): String? {
        if (!canStartSocialRequest(requester)) {
            return "Close the interface you're using first."
        }
        if (!canStartSocialRequest(other)) {
            return "${other.playerName} is currently busy."
        }
        return null
    }

    @JvmStatic
    fun duelBlockMessage(requester: Client, other: Client): String? {
        if (!canStartDuel(requester)) {
            return "Close the interface you're using first."
        }
        if (!canStartDuel(other)) {
            return "${other.playerName} is currently busy."
        }
        return null
    }
}
