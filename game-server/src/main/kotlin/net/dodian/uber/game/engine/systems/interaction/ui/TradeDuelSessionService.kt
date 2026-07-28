package net.dodian.uber.game.engine.systems.interaction.ui

import java.util.concurrent.atomic.AtomicLong
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.trade.TradeCompleteEvent
import net.dodian.uber.game.model.item.transaction.OfferTransactions
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.persistence.audit.TradeLog
import net.dodian.uber.game.persistence.audit.TradeSecurityAudit

/**
 * Runtime owner for trade/duel session transitions.
 *
 * This centralizes state-changing transitions so packet listeners and UI route
 * handlers can delegate instead of mutating raw trade/duel flags directly.
 */
object TradeDuelSessionService {
    private val nextTradeSessionId = AtomicLong(1L)

    @JvmStatic
    fun requestTrade(client: Client, targetSlot: Int) {
        if (client.inTrade) {
            return
        }
        if (!net.dodian.uber.game.engine.config.FeatureStateService.trading.get()) {
            client.sendMessage("Trading has been temporarily disabled")
            return
        }
        client.trade_reqId = targetSlot
        PlayerSocialApproachService.requestTrade(client, client.trade_reqId)
    }

    @JvmStatic
    fun requestDuel(client: Client, targetSlot: Int) {
        PlayerSocialApproachService.requestDuel(client, targetSlot)
    }

    @JvmStatic
    fun closeOpenTrade(client: Client) {
        if (!client.inTrade) {
            return
        }
        client.declineTrade()
        client.checkItemUpdate()
    }

    /** Starts one shared identity for a reciprocal trade before either UI opens. */
    @JvmStatic
    fun beginTradeSession(first: Client, second: Client): Long {
        val sessionId = nextTradeSessionId.getAndIncrement().coerceAtLeast(1L)
        initializeTradeState(first, second.slot, sessionId)
        initializeTradeState(second, first.slot, sessionId)
        return sessionId
    }

    /** Invalidates both confirmation screens whenever either offer changes. */
    @JvmStatic
    fun offerChanged(client: Client, other: Client?) {
        client.tradeOfferRevision++
        invalidateConfirmations(client)
        if (other != null && isReciprocalPair(client, other)) invalidateConfirmations(other)
    }

    @JvmStatic
    fun recordStageOneConfirmation(client: Client, other: Client): Boolean {
        if (!validatePair(client, other, requireConfirmed = false)) return false
        client.tradeConfirmed = true
        client.tradeConfirmedOwnRevision = client.tradeOfferRevision
        client.tradeConfirmedPartnerRevision = other.tradeOfferRevision
        return true
    }

    @JvmStatic
    fun confirmationsCurrent(first: Client, second: Client): Boolean =
        validatePair(first, second, requireConfirmed = true) &&
            first.tradeConfirmedOwnRevision == first.tradeOfferRevision &&
            first.tradeConfirmedPartnerRevision == second.tradeOfferRevision &&
            second.tradeConfirmedOwnRevision == second.tradeOfferRevision &&
            second.tradeConfirmedPartnerRevision == first.tradeOfferRevision

    /** Validates and commits both inventories/offers exactly once for the shared session. */
    @JvmStatic
    fun settleTrade(first: Client, second: Client): Boolean {
        val sessionId = first.tradeSessionId
        fun reject(reason: String): Boolean {
            TradeSecurityAudit.record(sessionId, first, second, "rejected", reason)
            first.sendMessage("Your trade could not be completed safely.")
            second.sendMessage("Your trade could not be completed safely.")
            return false
        }

        if (!confirmationsCurrent(first, second)) return reject("stale-or-invalid-confirmation")
        if (!first.tradeConfirmed2 || !second.tradeConfirmed2) return reject("stage-two-incomplete")
        if (first.tradeSettledSessionId == sessionId || second.tradeSettledSessionId == sessionId) {
            return reject("duplicate-settlement")
        }
        if (!offersValid(first, second)) return reject("invalid-offer")

        val firstOffer = copyOffer(first)
        val secondOffer = copyOffer(second)
        if (!OfferTransactions.settleTrade(first, second)) return reject("inventory-capacity")

        first.tradeSettledSessionId = sessionId
        second.tradeSettledSessionId = sessionId
        if (first.dbId > second.dbId) {
            TradeLog.recordTrade(first.dbId, second.dbId, firstOffer, secondOffer, true)
            GameEventBus.post(TradeCompleteEvent(first, second))
        } else {
            TradeLog.recordTrade(second.dbId, first.dbId, secondOffer, firstOffer, true)
            GameEventBus.post(TradeCompleteEvent(first, second))
        }
        first.completeTradeSettlementState()
        second.completeTradeSettlementState()
        TradeSecurityAudit.record(sessionId, first, second, "committed", "ok", firstOffer, secondOffer)
        return true
    }

    /** Refunds one side, or a reciprocal pair, once for the active session. */
    @JvmStatic
    fun cancelTrade(first: Client, second: Client?): Boolean {
        val sessionId = first.tradeSessionId
        if (!first.inTrade || sessionId <= 0L || first.tradeSettledSessionId == sessionId) return false
        if (second != null && (!isReciprocalPair(first, second) || second.tradeSettledSessionId == sessionId)) return false

        val firstOffer = copyOffer(first)
        val secondOffer = second?.let(::copyOffer) ?: emptyList()
        val refunded = if (second == null) {
            OfferTransactions.refundOffers(first)
        } else {
            OfferTransactions.refundOffers(first, second)
        }
        if (!refunded) {
            TradeSecurityAudit.record(sessionId, first, second, "rejected", "refund-capacity", firstOffer, secondOffer)
            return false
        }

        first.tradeSettledSessionId = sessionId
        second?.tradeSettledSessionId = sessionId
        TradeSecurityAudit.record(sessionId, first, second, "cancelled", "refunded", firstOffer, secondOffer)
        return true
    }

    @JvmStatic
    fun resetTradeSession(client: Client) {
        client.tradeSessionId = 0L
        client.tradeOfferRevision = 0L
        invalidateConfirmations(client)
    }

    @JvmStatic
    fun closeOpenDuel(client: Client) {
        if (!client.inDuel || client.duelFight) {
            return
        }
        client.declineDuel()
        client.checkItemUpdate()
    }

    @JvmStatic
    fun closeOnLogout(client: Client) {
        if (client.inTrade) {
            closeOpenTrade(client)
            return
        }
        if (client.inDuel && !client.duelFight) {
            closeOpenDuel(client)
        }
    }

    @JvmStatic
    fun confirmTradeStageOne(client: Client, other: Client): Boolean {
        return TradeDuelStateMachine.advanceTradeStageOne(client, other)
    }

    @JvmStatic
    fun confirmTradeStageTwo(client: Client, other: Client): Boolean {
        return TradeDuelStateMachine.advanceTradeStageTwo(client, other)
    }

    @JvmStatic
    fun confirmDuelStageOne(client: Client, other: Client): Boolean {
        return TradeDuelStateMachine.advanceDuelStageOne(client, other)
    }

    @JvmStatic
    fun confirmDuelStageTwo(client: Client, other: Client): Boolean {
        return TradeDuelStateMachine.advanceDuelStageTwo(client, other)
    }

    private fun initializeTradeState(client: Client, partnerSlot: Int, sessionId: Long) {
        client.trade_reqId = partnerSlot
        client.tradeSessionId = sessionId
        client.tradeOfferRevision = 0L
        client.tradeSettledSessionId = -1L
        invalidateConfirmations(client)
    }

    private fun invalidateConfirmations(client: Client) {
        client.tradeConfirmed = false
        client.tradeConfirmed2 = false
        client.tradeConfirmedOwnRevision = -1L
        client.tradeConfirmedPartnerRevision = -1L
    }

    private fun validatePair(first: Client, second: Client, requireConfirmed: Boolean): Boolean {
        if (!isReciprocalPair(first, second) || first.disconnected || second.disconnected) return false
        if (!first.inTrade || !second.inTrade || first.tradeSessionId <= 0L) return false
        return !requireConfirmed || (first.tradeConfirmed && second.tradeConfirmed)
    }

    private fun isReciprocalPair(first: Client, second: Client): Boolean =
        first !== second &&
            first.trade_reqId == second.slot && second.trade_reqId == first.slot &&
            first.tradeSessionId > 0L && first.tradeSessionId == second.tradeSessionId

    private fun offersValid(first: Client, second: Client): Boolean {
        val staffTrade = first.playerRights >= 2 || second.playerRights >= 2
        return sequenceOf(first, second).flatMap { it.offeredItems.asSequence() }.all { item ->
            item.id >= 0 && item.amount > 0 && item.amount <= first.maxItemAmount &&
                (staffTrade || Server.itemManager.isTradable(item.id))
        }
    }

    private fun copyOffer(client: Client) = java.util.concurrent.CopyOnWriteArrayList<GameItem>().apply {
        client.offeredItems.forEach { add(GameItem(it.id, it.amount)) }
    }
}
