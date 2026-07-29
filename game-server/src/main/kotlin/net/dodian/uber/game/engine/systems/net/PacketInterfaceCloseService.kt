package net.dodian.uber.game.engine.systems.net

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.engine.systems.interaction.PlayerTickThrottleService
import net.dodian.uber.game.engine.systems.interaction.ui.TradeDuelSessionService
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.api.content.ContentActionCancelReason
import net.dodian.uber.game.api.content.ContentActions

/**
 *
 * Moves all [Client] state mutations (banking, shop, duel, trade closures) and
 * [RemoveInterfaces] sends out of [ClickingStuffListener], leaving that listener
 * as a pure decode / delegate adapter.
 */
object PacketInterfaceCloseService {

    /**
     * Called after the single-byte payload is consumed by the listener.
     */
    @JvmStatic
    fun handle(client: Client) {
        // The client can close a widget after the outbound state has already
        // reset activeInterfaceId.  Always clear server-owned dialogue/action
        // state, otherwise the next NPC click is blocked by an invisible UI.
        ContentActions.cancel(
            player = client,
            reason = ContentActionCancelReason.INTERFACE_CLOSED,
            fullResetAnimation = false,
            resetCompatibilityState = true,
        )
        DialogueService.closeBlockingDialogue(client, closeInterfaces = false)
        client.contentRuntimeState.clearPendingInputState()
        net.dodian.uber.game.social.moderation.ModerationService.close(client)

        if (client.viewingAccountServices) {
            net.dodian.uber.game.ui.AccountServices.close(client)
        }
        if (client.IsBanking) {
            client.IsBanking = false
            client.bankSearchActive = false
            client.contentRuntimeState.clearPendingInputState()
            client.bankSearchQuery = ""
            client.checkItemUpdate()
        }
        if (client.isShopping) {
            client.MyShopID = -1
            client.checkItemUpdate()
        }
        if (client.checkBankInterface) {
            client.checkBankInterface = false
            client.checkItemUpdate()
        }
        if (client.bankStyleViewOpen) {
            client.clearBankStyleView()
            client.checkItemUpdate()
        }
        if (client.checkInv) {
            client.checkInv = false
            client.resetItems(3214)
        }
        if (client.isPartyInterface) {
            client.isPartyInterface = false
            client.checkItemUpdate()
        }
        if (client.inDuel && !client.duelFight) {
            TradeDuelSessionService.closeOpenDuel(client)
        }
        if (client.inTrade) {
            TradeDuelSessionService.closeOpenTrade(client)
        }
        client.contentRuntimeState.clearSelectedSkillGuideSkillId()
        client.send(RemoveInterfaces())
    }
}
