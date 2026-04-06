package net.dodian.uber.game.netty.listener.`in`

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class BankListenersBoundaryTest {
    @Test
    fun `bank listeners delegate mutations to packet banking service`() {
        val listenerFiles = listOf(
            "BankAllListener.java",
            "Bank5Listener.java",
            "Bank10Listener.java",
            "BankX1Listener.java",
            "BankX2Listener.java",
            "RemoveItemListener.java",
            "MoveItemsListener.java",
        )
        val forbiddenPatterns = listOf(
            "client.bankItem(",
            "client.fromBank(",
            "client.tradeItem(",
            "client.stakeItem(",
            "client.fromTrade(",
            "client.fromDuel(",
            "client.moveItems(",
            "client.sellItem(",
            "client.buyItem(",
            "client.addItem(",
            "client.deleteItem(",
            "client.remove(",
            "client.send(",
            "client.checkItemUpdate(",
            "client.XinterfaceID =",
            "client.XremoveID =",
            "client.XremoveSlot =",
            "client.enterAmountId =",
            "Balloons.offerItems(",
            "Balloons.removeOfferItems(",
        )

        listenerFiles.forEach { fileName ->
            val source = Files.readString(
                Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/$fileName"),
            )
            forbiddenPatterns.forEach { pattern ->
                assertFalse(
                    source.contains(pattern),
                    "$fileName still contains direct mutation pattern: $pattern",
                )
            }
        }
    }
}
