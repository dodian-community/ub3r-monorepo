package net.dodian.uber.game.content.buttons.banking

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendEnterName
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString

object BankInterfaceButtons : ButtonContent {
    override val requiredInterfaceId: Int = 5292

    override val buttonIds: IntArray = buildList {
        add(21011) // Toggle note (legacy)
        add(21010) // Withdraw as item (legacy mirror)
        add(5387) // Withdraw as note
        add(5386) // Withdraw as item
        add(50010) // Search button
        for (tab in 0..9) {
            add(50070 + tab * 4) // bank tab button
        }
    }.toIntArray()

    override fun onClick(client: Client, buttonId: Int): Boolean {
        if (!client.IsBanking || client.bankStyleViewOpen) {
            return true
        }

        if (buttonId in 50070..50106 && (buttonId - 50070) % 4 == 0) {
            val tab = (buttonId - 50070) / 4
            val actionIndex = client.lastButtonActionIndex
            if (actionIndex == 1 && tab > 0) {
                client.collapseBankTab(tab)
                return true
            }
            client.selectBankTab(tab)
            return true
        }

        when (buttonId) {
            21011, 5387 -> {
                client.takeAsNote = true
                client.send(SendMessage("You can now note items."))
            }

            21010, 5386 -> {
                client.takeAsNote = false
                client.send(SendMessage("You can no longer note items."))
            }

            50010 -> {
                if (client.bankSearchActive) {
                    client.clearBankSearch()
                } else {
                    client.bankSearchPendingInput = true
                    client.send(SendEnterName("Search bank:"))
                }
            }

            else -> return false
        }

        return true
    }
}
