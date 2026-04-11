package net.dodian.uber.game.systems.interaction

import net.dodian.uber.game.content.social.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client

object PlayerInteractionGuardService {
    @JvmStatic
    fun isDuelLocked(player: Client): Boolean =
        player.inDuel || player.duelConfirmed || player.duelConfirmed2 || player.duelFight

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
    fun canStartTrade(player: Client): Boolean =
        !isDuelLocked(player) && !hasBlockingInterface(player) && !hasActiveDialogue(player)

    @JvmStatic
    fun canStartDuel(player: Client): Boolean =
        !isDuelLocked(player) && !hasBlockingInterface(player) && !hasActiveDialogue(player)

    @JvmStatic
    fun canStartSocialRequest(player: Client): Boolean =
        !isDuelLocked(player) && !hasBlockingInterface(player) && !hasActiveDialogue(player)

    @JvmStatic
    fun canStartDialogue(player: Client): Boolean = !isDuelLocked(player) && !hasBlockingInterface(player)

    @JvmStatic
    fun canOpenBank(player: Client): Boolean = !isDuelLocked(player) && !hasActiveDialogue(player)

    @JvmStatic
    fun canOpenShop(player: Client): Boolean = !isDuelLocked(player) && !hasActiveDialogue(player)

    @JvmStatic
    fun blockingInteractionMessage(player: Client): String? {
        if (isDuelLocked(player)) {
            return "You can't do that while in a duel."
        }
        if (hasActiveDialogue(player) || hasBlockingInterface(player)) {
            return "Close the interface you're using first."
        }
        return null
    }

    @JvmStatic
    fun tradeBlockMessage(requester: Client, other: Client): String? {
        if (!canStartSocialRequest(requester)) {
            return blockingInteractionMessage(requester)
        }
        if (!canStartSocialRequest(other)) {
            return "${other.playerName} is currently busy."
        }
        return null
    }

    @JvmStatic
    fun duelBlockMessage(requester: Client, other: Client): String? {
        if (!canStartDuel(requester)) {
            return blockingInteractionMessage(requester)
        }
        if (!canStartDuel(other)) {
            return "${other.playerName} is currently busy."
        }
        return null
    }
}
