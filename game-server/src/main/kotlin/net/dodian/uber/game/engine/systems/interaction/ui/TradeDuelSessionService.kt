package net.dodian.uber.game.engine.systems.interaction.ui

import net.dodian.uber.game.model.entity.player.Client

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
        client.trade_reqId = targetSlot
        client.tradeReq(client.trade_reqId)
    }

    @JvmStatic
    fun requestLegacyTrade(client: Client, targetSlot: Int) {
        if (client.inTrade) {
            return
        }
        // Legacy opcode 128 routes through the historical duelReq path.
        client.duelReq(targetSlot)
    }

    @JvmStatic
    fun requestDuel(client: Client, targetSlot: Int) {
        client.duelReq(targetSlot)
    }

    @JvmStatic
    fun closeOpenTrade(client: Client) {
        if (!client.inTrade) {
            return
        }
        client.declineTrade()
        client.checkItemUpdate()
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
}
