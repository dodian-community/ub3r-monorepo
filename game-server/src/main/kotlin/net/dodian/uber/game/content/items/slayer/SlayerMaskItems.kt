package net.dodian.uber.game.content.items.slayer

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object SlayerMaskItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(11784, 11864, 11865)

    override fun onThirdClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        return when (itemId) {
            11864, 11865 -> {
                val needed = 8 - client.freeSlots()
                if (needed > 0) {
                    client.send(
                        SendMessage(
                            "you need $needed empty inventory slots to disassemble the ${client.GetItemName(itemId).lowercase()}."
                        )
                    )
                    true
                } else {
                    client.deleteItem(itemId, 1)
                    client.addItem(if (itemId == 11865) 11784 else 8921, 1)
                    client.addItem(4155, 1)
                    client.addItem(4156, 1)
                    client.addItem(4164, 1)
                    client.addItem(4166, 1)
                    client.addItem(4168, 1)
                    client.addItem(4551, 1)
                    client.addItem(6720, 1)
                    client.addItem(8923, 1)
                    client.checkItemUpdate()
                    client.send(SendMessage("you disassemble the ${client.GetItemName(itemId).lowercase()}."))
                    true
                }
            }

            11784 -> {
                val amountReturn = (2_000_000.0 * 0.7).toInt()
                if (client.addItem(995, amountReturn)) {
                    client.deleteItem(itemId, itemSlot, 1)
                    client.addItemSlot(8921, 1, itemSlot)
                    client.checkItemUpdate()
                } else {
                    client.send(SendMessage("You either need one free space or coins to not go beyond 2147million!"))
                }
                true
            }

            else -> false
        }
    }
}

