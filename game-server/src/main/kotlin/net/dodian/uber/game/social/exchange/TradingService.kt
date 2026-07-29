package net.dodian.uber.game.social.exchange

import net.dodian.uber.game.api.content.ContentInteraction
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.engine.lifecycle.PlayerDeferredLifecycleService
import net.dodian.uber.game.engine.systems.interaction.PlayerInteractionGuardService
import net.dodian.uber.game.engine.systems.interaction.ui.PlayerSocialApproachService
import net.dodian.uber.game.engine.systems.interaction.ui.TradeDuelSessionService
import net.dodian.uber.game.events.trade.TradeCancelEvent
import net.dodian.uber.game.events.trade.TradeRequestEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.netty.listener.out.ClearItemContainer
import net.dodian.uber.game.netty.listener.out.InventoryInterface
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.netty.listener.out.TradeItemsUpdate
import net.dodian.uber.game.engine.util.Misc
import org.slf4j.LoggerFactory

object TradingService {
    private val logger = LoggerFactory.getLogger(TradingService::class.java)

    @JvmStatic
    fun request(client: Client, targetSlot: Int) {
        val other = client.getClient(targetSlot) ?: return
        client.setFocus(other.position.x, other.position.y)
        if (!net.dodian.uber.game.engine.config.FeatureStateService.trading.get()) {
            client.sendMessage("Trading has been temporarily disabled")
            return
        }
        for (slot in PlayerRegistry.players.indices) {
            val duplicate = client.getClient(slot)
            if (slot != client.slot && client.validClient(slot) && duplicate.dbId > 0 && duplicate.dbId == client.dbId) {
                client.logout()
            }
        }
        PlayerInteractionGuardService.tradeBlockMessage(client, other)?.let {
            client.sendMessage(it)
            return
        }
        if (client.isBusy || other.isBusy) {
            client.sendMessage("That player is busy at the moment")
            client.trade_reqId = 0
            return
        }
        if (client.tradeLocked && other.playerRights < 1 || client.dbId == other.dbId) return

        client.trade_reqId = targetSlot
        if (!client.inTrade && other.tradeRequested && other.trade_reqId == client.slot) {
            TradeDuelSessionService.beginTradeSession(client, other)
            open(client)
            open(other)
        } else if (!client.inTrade && ContentInteraction.tryAcquireMs(client, ContentInteraction.TRADE_REQUEST, 1000L)) {
            client.tradeRequested = true
            GameEventBus.post(TradeRequestEvent(client, other))
            client.sendMessage("Sending trade request...")
            other.sendMessage("${client.playerName}:tradereq:")
        }
    }

    @JvmStatic
    fun open(client: Client) {
        val other = client.getClient(client.trade_reqId) ?: return decline(client, false)
        client.inTrade = true
        client.tradeRequested = false
        client.resetItems(3322)
        client.resetTItems(3415)
        client.resetOTItems(3416)
        client.send(InventoryInterface(3323, 3321))
        val prefix = when (other.playerRights) { 1 -> "@cr1@"; 2 -> "@cr2@"; else -> "" }
        client.send(SendString("Trading With: $prefix${other.playerName}", 3417))
        client.send(SendString("", 3431))
        client.send(SendString("Are you sure you want to make this trade?", 3535))
    }

    @JvmStatic
    @JvmOverloads
    fun decline(client: Client, tellOther: Boolean = true) {
        if (!client.inTrade || ExchangeRuntime.session(client) == null) return
        val other = client.getClient(client.trade_reqId)
        val cancelOther = tellOther && other != null && other.inTrade && other.trade_reqId == client.slot &&
            ExchangeRuntime.session(client, other, net.dodian.uber.game.api.plugin.social.ExchangeKind.TRADE) != null
        if (!TradeDuelSessionService.cancelTrade(client, if (cancelOther) other else null)) {
            logger.error("Unable to safely refund cancelled trade for player={} partner={}", client.playerName, other?.playerName ?: "none")
            client.sendMessage("Your trade could not be cancelled safely. Please contact staff.")
            return
        }
        finishDecline(client)
        if (cancelOther && other != null) {
            finishDecline(other)
            GameEventBus.post(TradeCancelEvent(client, other))
        }
    }

    @JvmStatic
    fun showConfirmation(client: Client) {
        val other = client.getClient(client.trade_reqId) ?: return decline(client)
        client.canOffer = false
        client.inTrade = true
        client.resetItems(3322)
        client.send(InventoryInterface(3443, 3321))
        client.send(ClearItemContainer(3538, 28))
        client.send(ClearItemContainer(3539, 28))
        val ownOffer = ExchangeRuntime.offers(client)
        val otherOffer = ExchangeRuntime.offers(other)
        if (ownOffer.size > 16) client.send(TradeItemsUpdate(3538, ownOffer))
        if (otherOffer.size > 16) client.send(TradeItemsUpdate(3539, otherOffer))
        client.send(SendString(formatOffer(client, ownOffer), 3557))
        client.send(SendString(formatOffer(client, otherOffer), 3558))
    }

    @JvmStatic
    fun completeSettlement(client: Client) {
        client.send(RemoveInterfaces())
        client.tradeResetNeeded = true
        PlayerDeferredLifecycleService.signalTradeFinalizeReady(client)
        client.faceTarget(-1)
    }

    @JvmStatic
    fun reset(client: Client) {
        ExchangeRuntime.remove(client)
        client.inTrade = false
        client.trade_reqId = 0
        client.canOffer = true
        client.tradeConfirmed = false
        client.tradeConfirmed2 = false
        TradeDuelSessionService.resetTradeSession(client)
        client.send(RemoveInterfaces())
        client.tradeResetNeeded = false
        client.send(SendString("Are you sure you want to make this trade?", 3535))
    }

    @JvmStatic
    fun lacksSettlementSpace(client: Client): Boolean {
        val other = client.getClient(client.trade_reqId) ?: return true
        val distinctStackables = hashSetOf<Int>()
        var spaces = 0
        ExchangeRuntime.offers(other).filter { it.amount > 0 }.forEach { item ->
            if (!item.isStackable() || distinctStackables.add(item.id)) spaces++
        }
        if (spaces > client.freeSpace) {
            val message = "${client.playerName} does not have enough space to hold items being traded."
            client.failer = message
            other.failer = message
            return true
        }
        return false
    }

    private fun finishDecline(client: Client) {
        client.send(RemoveInterfaces())
        client.inTrade = false
        client.canOffer = true
        client.tradeConfirmed = false
        client.tradeConfirmed2 = false
        ExchangeRuntime.remove(client)
        client.trade_reqId = -1
        PlayerSocialApproachService.cancel(client)
        TradeDuelSessionService.resetTradeSession(client)
        client.faceTarget(-1)
        client.checkItemUpdate()
    }

    private fun formatOffer(client: Client, offer: List<GameItem>): String {
        if (offer.isEmpty()) return "Absolutely nothing!"
        if (offer.size > 16) return ""
        return offer.joinToString("\\n") { item ->
            val amount = when {
                item.amount >= 1_000_000_000 -> "@gre@${item.amount / 1_000_000_000} billion @whi@(${Misc.format(item.amount)})"
                item.amount >= 1_000_000 -> "@gre@${item.amount / 1_000_000} million @whi@(${Misc.format(item.amount)})"
                item.amount >= 1_000 -> "@cya@${item.amount / 1_000}K @whi@(${Misc.format(item.amount)})"
                else -> Misc.format(item.amount)
            }
            client.getItemName(item.id) + if (item.amount > 1) " x $amount" else ""
        }
    }
}
