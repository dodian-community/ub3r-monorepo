package net.dodian.uber.game.engine.systems.interaction.ui

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.trade.TradeCompleteEvent
import net.dodian.uber.game.model.item.transaction.OfferTransactions
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.persistence.audit.TradeLog
import net.dodian.uber.game.persistence.audit.TradeSecurityAudit
import net.dodian.uber.game.social.exchange.TradingService
import net.dodian.uber.game.social.exchange.DuelingService
import net.dodian.uber.game.social.exchange.ExchangeRuntime
import net.dodian.uber.game.api.plugin.social.ExchangeKind
import java.util.UUID
import net.dodian.uber.game.api.plugin.social.ExchangeCommandResult
import net.dodian.uber.game.persistence.player.PlayerSaveService
import net.dodian.uber.game.persistence.player.PlayerSaveReason

/**
 * Runtime owner for trade/duel session transitions.
 *
 * This centralizes state-changing transitions so packet listeners and UI route
 * handlers can delegate instead of mutating raw trade/duel flags directly.
 */
object TradeDuelSessionService {
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
        TradingService.decline(client)
        client.checkItemUpdate()
    }

    /** Starts one shared identity for a reciprocal trade before either UI opens. */
    @JvmStatic
    fun beginTradeSession(first: Client, second: Client): Long {
        val session = ExchangeRuntime.create(ExchangeKind.TRADE, first, second)
        initializeTradeState(first, second.slot)
        initializeTradeState(second, first.slot)
        return auditId(session.id)
    }

    /** Invalidates both confirmation screens whenever either offer changes. */
    @JvmStatic
    fun offerChanged(client: Client, other: Client?) {
        invalidateConfirmations(client)
        if (other != null && isReciprocalPair(client, other)) invalidateConfirmations(other)
    }

    @JvmStatic
    fun recordStageOneConfirmation(client: Client, other: Client): Boolean {
        if (!validatePair(client, other, requireConfirmed = false)) return false
        val session = ExchangeRuntime.session(client, other, ExchangeKind.TRADE) ?: return false
        if (session.accept(ExchangeRuntime.participant(client), session.revision) !is ExchangeCommandResult.Applied) return false
        client.tradeConfirmed = true
        return true
    }

    @JvmStatic
    fun confirmationsCurrent(first: Client, second: Client): Boolean =
        validatePair(first, second, requireConfirmed = true) &&
            ExchangeRuntime.session(first, second, ExchangeKind.TRADE)?.stage ==
            net.dodian.uber.game.api.plugin.social.ExchangeStage.CONFIRM

    /** Both confirmation screens have been accepted for the same unchanged offer revision. */
    @JvmStatic
    fun settlementCurrent(first: Client, second: Client): Boolean =
        validatePair(first, second, requireConfirmed = true) &&
            ExchangeRuntime.session(first, second, ExchangeKind.TRADE)?.stage ==
            net.dodian.uber.game.api.plugin.social.ExchangeStage.SETTLING

    /** Validates and commits both inventories/offers exactly once for the shared session. */
    @JvmStatic
    fun settleTrade(first: Client, second: Client): Boolean {
        val existingSession = ExchangeRuntime.session(first, second, ExchangeKind.TRADE)
        val sessionId = existingSession?.id?.let(::auditId) ?: 0L
        fun reject(reason: String): Boolean {
            TradeSecurityAudit.record(sessionId, first, second, "rejected", reason)
            first.sendMessage("Your trade could not be completed safely.")
            second.sendMessage("Your trade could not be completed safely.")
            return false
        }

        if (!settlementCurrent(first, second)) return reject("stale-or-invalid-confirmation")
        if (!first.tradeConfirmed2 || !second.tradeConfirmed2) return reject("stage-two-incomplete")
        if (!offersValid(first, second)) return reject("invalid-offer")

        val firstOffer = copyOffer(first)
        val secondOffer = copyOffer(second)
        val session = ExchangeRuntime.session(first, second, ExchangeKind.TRADE) ?: return reject("missing-session")
        val settlementToken = session.id
        if (session.beginSettlement(settlementToken) is ExchangeCommandResult.Rejected) {
            return reject("duplicate-settlement")
        }
        val projection = OfferTransactions.projectReservedTrade(first, second)
        if (projection == null) {
            session.abortSettlement(settlementToken)
            return reject("inventory-capacity")
        }
        val queued = PlayerSaveService.requestPairedInventorySettlement(
            first,
            projection.firstAfter.itemIds,
            projection.firstAfter.amounts,
            second,
            projection.secondAfter.itemIds,
            projection.secondAfter.amounts,
            PlayerSaveReason.TRADE,
            Runnable {
                if (!OfferTransactions.publishProjection(first, second, projection)) {
                    TradeSecurityAudit.record(sessionId, first, second, "critical", "live-inventory-changed")
                    first.logout()
                    second.logout()
                    return@Runnable
                }
                session.complete(settlementToken)
                if (first.dbId > second.dbId) {
                    TradeLog.recordTrade(first.dbId, second.dbId, firstOffer, secondOffer, true)
                } else {
                    TradeLog.recordTrade(second.dbId, first.dbId, secondOffer, firstOffer, true)
                }
                GameEventBus.post(TradeCompleteEvent(first, second))
                TradingService.completeSettlement(first)
                TradingService.completeSettlement(second)
                TradeSecurityAudit.record(sessionId, first, second, "committed", "ok", firstOffer, secondOffer)
            },
            Runnable {
                session.abortSettlement(settlementToken)
                first.tradeConfirmed2 = false
                second.tradeConfirmed2 = false
                TradeSecurityAudit.record(sessionId, first, second, "rejected", "database-failure")
                first.sendMessage("Your trade could not be completed safely. Please try again.")
                second.sendMessage("Your trade could not be completed safely. Please try again.")
            },
        )
        if (!queued) {
            session.abortSettlement(settlementToken)
            return reject("settlement-busy")
        }
        return true
    }

    /** Cancels the session and releases its in-place reservations. */
    @JvmStatic
    fun cancelTrade(first: Client, second: Client?): Boolean {
        val session = ExchangeRuntime.session(first) ?: return false
        val sessionId = auditId(session.id)
        if (!first.inTrade || session.kind != ExchangeKind.TRADE) return false
        if (second != null && !isReciprocalPair(first, second)) return false

        val firstOffer = copyOffer(first)
        val secondOffer = second?.let(::copyOffer) ?: emptyList()
        session.cancel()
        ExchangeRuntime.remove(first)

        TradeSecurityAudit.record(sessionId, first, second, "cancelled", "reservations-released", firstOffer, secondOffer)
        return true
    }

    @JvmStatic
    fun resetTradeSession(client: Client) {
        invalidateConfirmations(client)
    }

    @JvmStatic
    fun isTradeSettled(client: Client): Boolean =
        ExchangeRuntime.session(client)?.let {
            it.kind == ExchangeKind.TRADE &&
                it.stage == net.dodian.uber.game.api.plugin.social.ExchangeStage.COMPLETE
        } == true

    @JvmStatic
    fun closeOpenDuel(client: Client) {
        if (!client.inDuel || client.duelFight) {
            return
        }
        DuelingService.decline(client)
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

    private fun initializeTradeState(client: Client, partnerSlot: Int) {
        client.trade_reqId = partnerSlot
        invalidateConfirmations(client)
    }

    private fun invalidateConfirmations(client: Client) {
        client.tradeConfirmed = false
        client.tradeConfirmed2 = false
    }

    private fun validatePair(first: Client, second: Client, requireConfirmed: Boolean): Boolean {
        if (!isReciprocalPair(first, second) || first.disconnected || second.disconnected) return false
        if (!first.inTrade || !second.inTrade) return false
        return !requireConfirmed || (first.tradeConfirmed && second.tradeConfirmed)
    }

    private fun isReciprocalPair(first: Client, second: Client): Boolean =
        first !== second &&
            first.trade_reqId == second.slot && second.trade_reqId == first.slot &&
            ExchangeRuntime.session(first, second, ExchangeKind.TRADE) != null

    private fun offersValid(first: Client, second: Client): Boolean {
        val staffTrade = first.playerRights >= 2 || second.playerRights >= 2
        return sequenceOf(first, second).flatMap { ExchangeRuntime.offers(it).asSequence() }.all { item ->
            item.id >= 0 && item.amount > 0 && item.amount <= first.maxItemAmount &&
                (staffTrade || Server.itemManager.isTradable(item.id))
        }
    }

    private fun copyOffer(client: Client) = java.util.concurrent.CopyOnWriteArrayList<GameItem>().apply {
        ExchangeRuntime.offers(client).forEach { add(GameItem(it.id, it.amount)) }
    }

    private fun auditId(id: UUID): Long = id.mostSignificantBits xor id.leastSignificantBits
}
