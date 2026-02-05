package net.dodian.uber.game.content.buttons.banking

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString

object BankInterfaceButtons : ButtonContent {
    override val requiredInterfaceId: Int = 5292

    override val buttonIds: IntArray = intArrayOf(
        5387, // Toggle note
        5386, // Bank all
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        if (!client.IsBanking) {
            return true
        }

        when (buttonId) {
            21011 -> {
                client.takeAsNote = !client.takeAsNote
                client.send(SendString(if (client.takeAsNote) "No Note" else "Note", 5389))
                client.send(SendMessage(if (client.takeAsNote) "You can now note items." else "You can no longer note items."))
            }

            21010 -> {
                if (client.freeSlots() < 28) {
                    for (i in 0 until 28) {
                        if (client.playerItems[i] > 0) {
                            client.bankItem(client.playerItems[i] - 1, i, client.playerItemsN[i])
                        }
                    }
                    client.send(SendMessage("You bank all your items!"))
                    client.checkItemUpdate()
                } else {
                    client.send(SendMessage("You do not have anything that can be banked!"))
                }
            }

            else -> return false
        }

        return true
    }
}

