package net.dodian.uber.game.content.interfaces.trade

import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.systems.api.content.ContentInteraction
import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding
import org.slf4j.LoggerFactory

object TradeInterfaceButtons : InterfaceButtonContent {
    private val logger = LoggerFactory.getLogger(TradeInterfaceButtons::class.java)

    override val bindings =
        listOf(
            buttonBinding(-1, 0, "trade.confirm.stage_one", intArrayOf(TradeComponents.CONFIRM_STAGE_ONE_BUTTON)) { client, _ ->
                try {
                    val other = client.getClient(client.trade_reqId)
                    if (other == null || !client.validClient(client.trade_reqId) || !client.inTrade ||
                        !ContentInteraction.tryAcquireMs(client, ContentInteraction.TRADE_CONFIRM_STAGE_ONE, 600L)
                    ) {
                        return@buttonBinding true
                    }
                    if (client.inTrade && !client.tradeConfirmed) {
                        client.tradeConfirmed = true
                        if (other.tradeConfirmed) {
                            if (other.hasTradeSpace() || client.hasTradeSpace()) {
                                client.send(SendMessage(client.failer))
                                other.send(SendMessage(client.failer))
                                client.declineTrade()
                                return@buttonBinding true
                            }
                            client.confirmScreen()
                            other.confirmScreen()
                        } else {
                            client.send(SendString("Waiting for other player...", 3431))
                            if (client.validClient(client.trade_reqId)) {
                                other.send(SendString("Other player has accepted", 3431))
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Trade button issue!", e)
                }
                true
            },
            buttonBinding(-1, 1, "trade.confirm.stage_two", intArrayOf(TradeComponents.CONFIRM_STAGE_TWO_BUTTON)) { client, _ ->
                try {
                    val other = client.getClient(client.trade_reqId)
                    if (other == null || !client.validClient(client.trade_reqId) || !client.inTrade ||
                        !ContentInteraction.tryAcquireMs(client, ContentInteraction.TRADE_CONFIRM_STAGE_TWO, 600L)
                    ) {
                        return@buttonBinding true
                    }
                    if (client.inTrade && client.tradeConfirmed && other.tradeConfirmed && !client.tradeConfirmed2) {
                        client.tradeConfirmed2 = true
                        if (other.tradeConfirmed2) {
                            client.giveItems()
                            other.giveItems()
                        } else {
                            other.send(SendString("Other player has accepted.", 3535))
                            client.send(SendString("Waiting for other player...", 3535))
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Trade button issue!", e)
                }
                true
            },
        )
}
