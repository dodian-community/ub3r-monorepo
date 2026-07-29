package net.dodian.uber.game.engine.systems.interaction.ui

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.social.exchange.TradingService
import net.dodian.uber.game.social.exchange.DuelingService
import net.dodian.uber.game.social.exchange.ExchangeRuntime
import net.dodian.uber.game.api.plugin.social.ExchangeKind
import net.dodian.uber.game.api.plugin.social.ExchangeCommandResult

object TradeDuelStateMachine {
    @JvmStatic
    fun advanceTradeStageOne(client: Client, other: Client): Boolean {
        if (!client.inTrade || client.tradeConfirmed) {
            return true
        }
        if (!TradeDuelSessionService.recordStageOneConfirmation(client, other)) {
            return true
        }
        if (other.tradeConfirmed) {
            if (!TradeDuelSessionService.confirmationsCurrent(client, other)) {
                client.sendMessage("The trade changed; please review it again.")
                other.sendMessage("The trade changed; please review it again.")
                return true
            }
            if (TradingService.lacksSettlementSpace(other) || TradingService.lacksSettlementSpace(client)) {
                client.sendMessage(client.failer)
                other.sendMessage(client.failer)
                TradeDuelSessionService.closeOpenTrade(client)
                return true
            }
            TradingService.showConfirmation(client)
            TradingService.showConfirmation(other)
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
        val session = ExchangeRuntime.session(client, other, ExchangeKind.TRADE) ?: return true
        if (session.accept(ExchangeRuntime.participant(client), session.revision) !is ExchangeCommandResult.Applied) {
            return true
        }
        if (other.tradeConfirmed2) {
            TradeDuelSessionService.settleTrade(client, other)
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
        if (!canAttackWithDuelRules(client)) {
            client.sendString("You don't have weapons/spells for rules!", 31009)
            client.sendMessage("You don't have the right equipment to attack with the enabled combat styles!")
            return true
        }
        if (!canAttackWithDuelRules(other)) {
            client.sendString("Opponent doesn't have weapons/spells!", 31009)
            client.sendMessage("Your opponent doesn't have the right equipment to attack with the enabled combat styles!")
            return true
        }
        val session = ExchangeRuntime.session(client, other, ExchangeKind.DUEL) ?: return true
        if (session.accept(ExchangeRuntime.participant(client), session.revision) !is ExchangeCommandResult.Applied) return true
        client.duelConfirmed = true
        if (!other.duelConfirmed) {
            client.sendString("Waiting for other player...", 31009)
            other.sendString("Other player has accepted.", 31009)
            return true
        }

        if (client.duelRule[0] && client.duelRule[1] && client.duelRule[2]) {
            TradeDuelSessionService.closeOpenDuel(client)
            client.sendMessage("At least one combat style must be enabled!")
            other.sendMessage("At least one combat style must be enabled!")
            return true
        }
        if (DuelingService.hasEnoughSpace(client) || DuelingService.hasEnoughSpace(other)) {
            client.sendMessage(client.failer)
            other.sendMessage(client.failer)
            TradeDuelSessionService.closeOpenDuel(client)
            return true
        }

        client.canOffer = false
        DuelingService.showConfirmation(client)
        DuelingService.showConfirmation(other)
        return true
    }

    @JvmStatic
    fun advanceDuelStageTwo(client: Client, other: Client): Boolean {
        if (!client.inDuel || client.duelConfirmed2) {
            return true
        }
        client.canOffer = false
        client.duelConfirmed2 = true
        val session = ExchangeRuntime.session(client, other, ExchangeKind.DUEL) ?: return true
        if (session.accept(ExchangeRuntime.participant(client), session.revision) !is ExchangeCommandResult.Applied) return true
        if (other.duelConfirmed2) {
            if (session.beginActive() !is ExchangeCommandResult.Applied) return true
            DuelingService.removeEquipment(client)
            DuelingService.removeEquipment(other)
            if (!canAttackWithDuelRules(client) || !canAttackWithDuelRules(other)) {
                val msg = "You don't have the right equipment to attack with the enabled combat styles!"
                client.sendMessage(msg)
                other.sendMessage(msg)
                DuelingService.decline(client)
                return true
            }
            DuelingService.start(client)
            DuelingService.start(other)
        } else {
            client.sendString("Waiting for other player...", 31526)
            other.sendString("Other player has accepted", 31526)
        }
        return true
    }

    @JvmStatic
    fun canAttackWithDuelRules(player: Client): Boolean {
        val noMelee = player.duelRule[1]
        val noRanged = player.duelRule[0]
        val noMagic = player.duelRule[2]

        if (!noMelee) return true
        if (!noRanged && player.contentRuntimeState.isCombatUsingBow()) return true
        if (!noMagic && (player.magicId >= 0 || (player.hasStaff() && player.autocast_spellIndex >= 0))) return true
        return false
    }
}
