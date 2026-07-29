package net.dodian.uber.game.social.exchange

import net.dodian.uber.game.Server
import net.dodian.uber.game.api.content.ContentInteraction
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.engine.systems.interaction.ui.TradeDuelSessionService
import net.dodian.uber.game.api.plugin.social.runtime.ExchangeAmount
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendString
import org.slf4j.LoggerFactory

/** Kotlin owner for inventory <-> exchange escrow mutations and their UI synchronization. */
object ExchangeOfferService {
    private val logger = LoggerFactory.getLogger(ExchangeOfferService::class.java)

    @JvmStatic
    fun offerTrade(client: Client, itemId: Int, inventorySlot: Int, requestedAmount: Int) {
        if (!ContentInteraction.tryAcquireMs(client, ContentInteraction.TRADE_CONFIRM_STAGE_TWO, 200L)) return
        val other = reciprocalTradePartner(client) ?: return TradingService.decline(client)
        val amount = inventoryAmount(client, itemId, inventorySlot, requestedAmount) ?: return
        if (!client.canOffer) return TradingService.decline(client)
        if (!Server.itemManager.isTradable(itemId) && client.playerRights < 2 && other.playerRights < 2) {
            client.sendMessage("You can't trade this item")
            return
        }
        if (!ExchangeRuntime.reserve(client, inventorySlot, itemId, amount)) return
        TradeDuelSessionService.offerChanged(client, other)
        refreshTrade(client, other)
    }

    @JvmStatic
    fun withdrawTrade(client: Client, itemId: Int, offerSlot: Int, requestedAmount: Int) {
        if (!ContentInteraction.tryAcquireMs(client, ContentInteraction.TRADE_CONFIRM_STAGE_ONE, 200L)) return
        val other = reciprocalTradePartner(client) ?: return TradingService.decline(client)
        if (!client.canOffer) return TradingService.decline(client)
        val amount = offerAmount(client, itemId, offerSlot, requestedAmount) ?: return
        if (!ExchangeRuntime.withdraw(client, offerSlot, itemId, amount)) return
        TradeDuelSessionService.offerChanged(client, other)
        refreshTrade(client, other)
    }

    @JvmStatic
    fun offerDuel(client: Client, itemId: Int, inventorySlot: Int, requestedAmount: Int) {
        if (!ContentInteraction.tryAcquireMs(client, ContentInteraction.DUEL_CONFIRM_STAGE_ONE, 200L)) return
        val other = reciprocalDuelPartner(client) ?: return DuelingService.decline(client)
        if (!client.canOffer) return DuelingService.decline(client)
        val amount = inventoryAmount(client, itemId, inventorySlot, requestedAmount) ?: return
        if (!Server.itemManager.isTradable(itemId)) {
            client.sendMessage("You can't trade that item")
            return
        }
        if (!ExchangeRuntime.reserve(client, inventorySlot, itemId, amount)) return
        duelOfferChanged(client, other)
    }

    @JvmStatic
    fun withdrawDuel(client: Client, itemId: Int, offerSlot: Int, requestedAmount: Int) {
        if (!ContentInteraction.tryAcquireMs(client, ContentInteraction.DUEL_CONFIRM_STAGE_TWO, 200L)) return
        val other = reciprocalDuelPartner(client) ?: return DuelingService.decline(client)
        if (!client.canOffer) return DuelingService.decline(client)
        val amount = offerAmount(client, itemId, offerSlot, requestedAmount) ?: return
        if (!ExchangeRuntime.withdraw(client, offerSlot, itemId, amount)) return
        duelOfferChanged(client, other)
    }

    private fun reciprocalTradePartner(client: Client): Client? {
        if (!client.inTrade || !client.validClient(client.trade_reqId)) return null
        val other = client.getClient(client.trade_reqId) ?: return null
        return other.takeIf {
            it.inTrade && it.trade_reqId == client.slot &&
                ExchangeRuntime.session(client, it, net.dodian.uber.game.api.plugin.social.ExchangeKind.TRADE) != null
        }
    }

    private fun reciprocalDuelPartner(client: Client): Client? {
        if (!client.inDuel || !client.validClient(client.duel_with)) return null
        val other = client.getClient(client.duel_with) ?: return null
        return other.takeIf { it.inDuel && it.duel_with == client.slot && !it.duelFight && !client.duelFight }
    }

    private fun inventoryAmount(client: Client, itemId: Int, slot: Int, requested: Int): Int? {
        if (requested <= 0 || slot !in client.playerItems.indices || client.playerItems[slot] != itemId + 1) return null
        val available = if (Server.itemManager.isStackable(itemId)) {
            ExchangeRuntime.availableAt(client, slot, itemId)
        } else {
            client.playerItems.indices.sumOf {
                if (client.playerItems[it] == itemId + 1) ExchangeRuntime.availableAt(client, it, itemId) else 0
            }.coerceAtLeast(0)
        }
        return ExchangeAmount.resolve(requested, available)
    }

    private fun offerAmount(client: Client, itemId: Int, slot: Int, requested: Int): Int? {
        val offers = ExchangeRuntime.offers(client)
        if (requested <= 0 || slot !in offers.indices) return null
        val selected = offers[slot]
        if (selected.id != itemId) {
            logger.warn("Rejected exchange withdrawal item mismatch player={} slot={} requested={} actual={}",
                client.playerName, slot, itemId, selected.id)
            return null
        }
        val available = if (selected.isStackable()) {
            selected.amount.coerceAtLeast(0)
        } else {
            offers.count { it.id == itemId }
        }
        return ExchangeAmount.resolve(requested, available)
    }

    private fun refreshTrade(client: Client, other: Client) {
        client.resetItems(3322)
        client.resetTItems(3415)
        other.resetOTItems(3416)
        client.send(SendString("", 3431))
        other.send(SendString("", 3431))
    }

    private fun duelOfferChanged(client: Client, other: Client) {
        client.duelConfirmed = false
        other.duelConfirmed = false
        val cycle = GameCycleClock.currentCycle()
        client.lastDuelItemChangeCycle = cycle
        other.lastDuelItemChangeCycle = cycle
        client.resetItems(3214)
        client.resetItems(3322)
        other.resetItems(3214)
        other.resetItems(3322)
        DuelingService.refreshOffer(client)
        DuelingService.refreshOffer(other)
        client.send(SendString("", 6684))
        other.send(SendString("", 6684))
    }
}
