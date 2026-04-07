package net.dodian.uber.game.events.trade

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.player.Client

/** Fired when one player sends a trade request to another. */
data class TradeRequestEvent(
    val requester: Client,
    val target: Client,
) : GameEvent

/** Fired when both players accept and the exchange completes. */
data class TradeCompleteEvent(
    val playerA: Client,
    val playerB: Client,
) : GameEvent

/** Fired when a trade session is declined or cancelled by either party. */
data class TradeCancelEvent(
    val canceller: Client,
    val other: Client,
) : GameEvent

