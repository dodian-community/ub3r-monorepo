package net.dodian.uber.game.content.buttons.trade

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString

object TradeConfirmButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(13092, 13218)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            13092 -> {
                try {
                    val other = client.getClient(client.trade_reqId)
                    if (other == null || !client.validClient(client.trade_reqId) || System.currentTimeMillis() - client.lastButton < 600 || !client.inTrade) {
                        return true
                    }
                    client.lastButton = System.currentTimeMillis()
                    if (client.inTrade && !client.tradeConfirmed) {
                        client.tradeConfirmed = true
                        if (other.tradeConfirmed) {
                            if (other.hasTradeSpace() || client.hasTradeSpace()) {
                                client.send(SendMessage(client.failer))
                                other.send(SendMessage(client.failer))
                                client.declineTrade()
                                return true
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
                    System.out.println("Trade button issue! $e")
                }
                return true
            }

            13218 -> {
                try {
                    val other = client.getClient(client.trade_reqId)
                    if (other == null || !client.validClient(client.trade_reqId) || System.currentTimeMillis() - client.lastButton < 600 || !client.inTrade) {
                        return true
                    }
                    client.lastButton = System.currentTimeMillis()
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
                    System.out.println("Trade button issue! $e")
                }
                return true
            }
        }
        return false
    }
}

