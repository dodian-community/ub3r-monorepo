package net.dodian.uber.game.engine.systems.net

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.engine.systems.interaction.PlayerTickThrottleService
import net.dodian.uber.game.engine.systems.interaction.ui.TradeDuelSessionService

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
        if (client.viewingAccountServices) {
            net.dodian.uber.game.ui.AccountServices.close(client)
        }
        if (client.IsBanking) {
            client.IsBanking = false
            client.bankSearchActive = false
            client.bankSearchPendingInput = false
            client.bankSearchQuery = ""
            client.checkItemUpdate()
            client.send(RemoveInterfaces())
        }
        if (client.isShopping) {
            client.MyShopID = -1
            client.checkItemUpdate()
            client.send(RemoveInterfaces())
        }
        if (client.checkBankInterface) {
            client.checkBankInterface = false
            client.checkItemUpdate()
            client.send(RemoveInterfaces())
        }
        if (client.bankStyleViewOpen) {
            client.clearBankStyleView()
            client.checkItemUpdate()
            client.send(RemoveInterfaces())
        }
        if (client.isPartyInterface) {
            client.isPartyInterface = false
            client.checkItemUpdate()
            client.send(RemoveInterfaces())
        }
        if (client.inDuel && !client.duelFight) {
            TradeDuelSessionService.closeOpenDuel(client)
        }
        if (client.inTrade) {
            TradeDuelSessionService.closeOpenTrade(client)
        }
        if (client.currentSkill >= 0) {
            client.currentSkill = -1
        }
    }
}
