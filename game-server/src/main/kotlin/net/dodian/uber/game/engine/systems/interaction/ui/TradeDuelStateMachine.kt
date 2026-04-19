package net.dodian.uber.game.engine.systems.interaction.ui

import net.dodian.uber.game.model.entity.player.Client

object TradeDuelStateMachine {
    @JvmStatic
    fun advanceTradeStageOne(client: Client, other: Client): Boolean {
        if (!client.inTrade || client.tradeConfirmed) {
            return true
        }
        client.tradeConfirmed = true
        if (other.tradeConfirmed) {
            if (other.hasTradeSpace() || client.hasTradeSpace()) {
                client.sendMessage(client.failer)
                other.sendMessage(client.failer)
                TradeDuelSessionService.closeOpenTrade(client)
                return true
            }
            client.confirmScreen()
            other.confirmScreen()
            return true
        }
        client.sendString("Waiting for other player...", 3431)
        if (client.validClient(client.trade_reqId)) {
            other.sendString("Other player has accepted", 3431)
        }
        return true
    }

    @JvmStatic
    fun advanceTradeStageTwo(client: Client, other: Client): Boolean {
        if (!client.inTrade || !client.tradeConfirmed || !other.tradeConfirmed || client.tradeConfirmed2) {
            return true
        }
        client.tradeConfirmed2 = true
        if (other.tradeConfirmed2) {
            client.giveItems()
            other.giveItems()
        } else {
            other.sendString("Other player has accepted.", 3535)
            client.sendString("Waiting for other player...", 3535)
        }
        return true
    }

    @JvmStatic
    fun advanceDuelStageOne(client: Client, other: Client): Boolean {
        if (!client.inDuel || client.duelConfirmed) {
            return true
        }
        client.duelConfirmed = true
        if (!other.duelConfirmed) {
            client.sendString("Waiting for other player...", 6684)
            other.sendString("Other player has accepted.", 6684)
            return true
        }

        if (client.duelRule[0] && client.duelRule[1] && client.duelRule[2]) {
            TradeDuelSessionService.closeOpenDuel(client)
            client.sendMessage("At least one combat style must be enabled!")
            other.sendMessage("At least one combat style must be enabled!")
            return true
        }
        if (client.hasEnoughSpace() || other.hasEnoughSpace()) {
            client.sendMessage(client.failer)
            other.sendMessage(client.failer)
            TradeDuelSessionService.closeOpenDuel(client)
            return true
        }

        client.canOffer = false
        client.confirmDuel()
        other.confirmDuel()
        return true
    }

    @JvmStatic
    fun advanceDuelStageTwo(client: Client, other: Client): Boolean {
        if (!client.inDuel || client.duelConfirmed2) {
            return true
        }
        client.canOffer = false
        client.duelConfirmed2 = true
        if (other.duelConfirmed2) {
            client.removeEquipment()
            other.removeEquipment()
            client.startDuel()
            other.startDuel()
        } else {
            client.sendString("Waiting for other player...", 6571)
            other.sendString("Other player has accepted", 6571)
        }
        return true
    }
}
