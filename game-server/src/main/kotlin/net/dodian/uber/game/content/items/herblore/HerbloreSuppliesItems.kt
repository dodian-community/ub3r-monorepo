package net.dodian.uber.game.content.items.herblore

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client

object HerbloreSuppliesItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(11877, 11879, 12859)

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        when (itemId) {
            11877 -> {
                client.deleteItem(11877, itemSlot, 1)
                if (!client.playerHasItem(230)) {
                    client.addItemSlot(230, 100, itemSlot)
                } else {
                    client.addItem(230, 100)
                }
                return true
            }

            11879 -> {
                client.deleteItem(11879, itemSlot, 1)
                if (!client.playerHasItem(228)) {
                    client.addItemSlot(228, 100, itemSlot)
                } else {
                    client.addItem(228, 100)
                }
                return true
            }

            12859 -> {
                client.deleteItem(12859, itemSlot, 1)
                if (!client.playerHasItem(222)) {
                    client.addItemSlot(222, 100, itemSlot)
                } else {
                    client.addItem(222, 100)
                }
                return true
            }
        }
        return false
    }
}
