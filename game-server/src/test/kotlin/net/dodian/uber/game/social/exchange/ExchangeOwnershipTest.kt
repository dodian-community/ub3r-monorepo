package net.dodian.uber.game.social.exchange

import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExchangeOwnershipTest {
    @Test
    fun `client exposes no trade or duel command methods`() {
        val removedMethods = setOf(
            "tradeReq", "openTrade", "declineTrade", "tradeItem", "fromTrade", "confirmScreen",
            "giveItems", "resetTrade", "duelReq", "openDuel", "declineDuel", "stakeItem",
            "fromDuel", "confirmDuel", "startDuel", "resetDuel", "DuelVictory", "acceptDuelWon",
            "toggleDuelRule", "toggleDuelBodyRule", "checkGameitemAmount",
        )
        val remaining = Client::class.java.declaredMethods.map { it.name }.toSet().intersect(removedMethods)
        assertTrue(remaining.isEmpty(), "Client still owns exchange command methods: $remaining")
    }
}
