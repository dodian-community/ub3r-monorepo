package net.dodian.uber.game.content.buttons.ui

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.runtime.action.PlayerActionCancellationService
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason

object CloseInterfaceButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        83051,
        9118,
        19022,
        50001,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        val wasBanking = client.IsBanking
        val wasBankPreview = client.checkBankInterface
        val wasItemListPreview = client.bankStyleViewOpen
        val wasPartyInterface = client.isPartyInterface
        val wasShopping = client.isShopping
        PlayerActionCancellationService.cancel(
            player = client,
            reason = PlayerActionCancelReason.INTERFACE_CLOSED,
            fullResetAnimation = false,
            resetCompatibilityState = true,
        )
        DialogueService.closeBlockingDialogue(client, closeInterfaces = true)
        if (client.refundSlot != -1) {
            client.refundSlot = -1
        }
        if (client.herbMaking != -1) {
            client.herbMaking = -1
        }
        var refreshItems = false
        if (wasBanking) {
            client.IsBanking = false
            client.bankSearchActive = false
            client.bankSearchPendingInput = false
            client.bankSearchQuery = ""
            client.currentBankTab = 0
            refreshItems = true
        }
        if (wasBankPreview) {
            client.checkBankInterface = false
            refreshItems = true
        }
        if (wasItemListPreview) {
            client.clearBankStyleView()
            refreshItems = true
        }
        if (wasPartyInterface) {
            client.isPartyInterface = false
            refreshItems = true
        }
        if (wasShopping) {
            client.MyShopID = -1
            refreshItems = true
        }
        if (refreshItems) {
            client.checkItemUpdate()
        }
        return true
    }
}
