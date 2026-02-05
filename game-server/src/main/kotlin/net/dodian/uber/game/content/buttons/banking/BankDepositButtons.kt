package net.dodian.uber.game.content.buttons.banking

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object BankDepositButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        89223, // Mystic "Deposit inventory" button (alt id)
        50004, // Mystic "Deposit inventory" button
        50007, // Mystic "Deposit worn items" button
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        if (!client.IsBanking) {
            return true
        }

        when (buttonId) {
            89223, 50004 -> {
                for (i in client.playerItems.indices) {
                    if (client.playerItems[i] > 0) {
                        client.bankItem(client.playerItems[i] - 1, i, client.playerItemsN[i])
                    }
                }
                client.send(SendMessage("You deposit all your items."))
                client.checkItemUpdate()
                return true
            }

            50007 -> {
                val equipment = client.equipment
                val equipmentN = client.equipmentN
                for (i in equipment.indices) {
                    val equipId = equipment[i]
                    val equipAmount = equipmentN[i]
                    if (equipId > 0 && equipAmount > 0) {
                        if (client.hasSpace()) {
                            if (client.remove(i, false)) {
                                client.addItem(equipId, equipAmount)
                                client.bankItem(equipId, client.GetItemSlot(equipId), equipAmount)
                            }
                        }
                    }
                }
                client.send(SendMessage("You deposit your worn items."))
                client.checkItemUpdate()
                return true
            }

            else -> return false
        }
    }
}

