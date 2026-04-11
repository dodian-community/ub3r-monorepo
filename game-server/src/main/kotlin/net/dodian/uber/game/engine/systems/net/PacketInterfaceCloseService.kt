package net.dodian.uber.game.engine.systems.net

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.engine.systems.interaction.PlayerTickThrottleService

/**
 * Kotlin service for the miscellaneous interface-close packet (opcode 130).
 *
 * Moves all [Client] state mutations (banking, shop, duel, trade closures) and
 * [RemoveInterfaces] sends out of [ClickingStuffListener], leaving that listener
 * as a pure decode / delegate adapter.
 */
object PacketInterfaceCloseService {

    /**
     * Handles the full close-interfaces packet logic.
     * Called after the single-byte payload is consumed by the listener.
     */
    @JvmStatic
    fun handle(client: Client) {
        if (client.IsBanking) {
            client.IsBanking = false
            client.checkItemUpdate()
            client.send(RemoveInterfaces())
        }
        if (client.isShopping()) {
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
            val other = client.getClient(client.duel_with)
            if (other == null || !client.validClient(client.duel_with) ||
                !PlayerTickThrottleService.tryAcquireMs(client, PlayerTickThrottleService.DUEL_REQUEST, 600L)
            ) {
                return
            }
            client.declineDuel()
            client.checkItemUpdate()
        }
        if (client.inTrade) {
            val other = client.getClient(client.trade_reqId)
            if (other == null || !client.validClient(client.trade_reqId) ||
                !PlayerTickThrottleService.tryAcquireMs(client, PlayerTickThrottleService.TRADE_REQUEST, 600L)
            ) {
                return
            }
            client.declineTrade()
            client.checkItemUpdate()
        }
        if (client.currentSkill >= 0) {
            client.currentSkill = -1 // Close skill menu interface
        }
    }
}

