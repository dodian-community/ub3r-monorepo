package net.dodian.uber.game.persistence.audit

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.social.exchange.ExchangeRuntime
import org.slf4j.LoggerFactory

/** Structured security trail for every terminal trade decision. */
object TradeSecurityAudit {
    private val logger = LoggerFactory.getLogger("net.dodian.consoleaudit.trade")

    @JvmStatic
    @JvmOverloads
    fun record(
        sessionId: Long,
        first: Client,
        second: Client?,
        result: String,
        reason: String,
        firstItems: Iterable<GameItem> = ExchangeRuntime.offers(first),
        secondItems: Iterable<GameItem> = second?.let(ExchangeRuntime::offers) ?: emptyList(),
    ) {
        if (logger.isInfoEnabled) {
            logger.info(
                "TRADE_SECURITY session={} first={} second={} firstOffer={} secondOffer={} result={} reason={}",
                sessionId,
                playerRef(first),
                second?.let(::playerRef) ?: "none",
                fingerprint(firstItems),
                if (second == null) "none" else fingerprint(secondItems),
                result,
                reason,
            )
        }
    }

    private fun playerRef(client: Client): String = "${client.dbId}:${sanitize(client.playerName)}:${client.slot}"

    private fun fingerprint(items: Iterable<GameItem>): String =
        items
            .groupBy { it.id }
            .toSortedMap()
            .entries
            .joinToString(",") { (id, entries) ->
                val amount = entries.sumOf { it.amount.toLong() }.coerceAtMost(Int.MAX_VALUE.toLong())
                "$id:$amount"
            }
            .ifEmpty { "empty" }

    private fun sanitize(value: String?): String = value.orEmpty().replace(Regex("[^A-Za-z0-9 _-]"), "_").take(24)
}
