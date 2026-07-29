package net.dodian.uber.game.social.exchange

import java.util.WeakHashMap
import net.dodian.uber.game.model.entity.player.Client

/**
 * Transitional player-facing state that is not part of the authoritative
 * exchange aggregate (requests and client-interface compatibility flags).
 * Keeping it outside Client prevents the entity from owning trade/duel data.
 */
class ExchangePlayerState {
    var duelRequested = false
    var inDuel = false
    var duelConfirmed = false
    var duelConfirmed2 = false
    var duelFight = false
    var duelWin = false
    var lastDuelItemChangeCycle = 0L
    var duelPartnerSlot = 0
    var tradeRequested = false
    var inTrade = false
    var canOffer = true
    var tradeConfirmed = false
    var tradeConfirmed2 = false
    var tradeResetNeeded = false
    var tradePartnerSlot = 0
    var duelRule = BooleanArray(11)
    val duelBodyRules = BooleanArray(11)
}

object ExchangePlayerStateRegistry {
    private val states = WeakHashMap<Client, ExchangePlayerState>()

    @JvmStatic
    fun state(client: Client): ExchangePlayerState =
        states.getOrPut(client) { ExchangePlayerState() }

    @JvmStatic
    fun remove(client: Client) {
        states.remove(client)
    }
}
