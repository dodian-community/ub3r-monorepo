package net.dodian.uber.game.content.items.consumables

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object DrinkItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(
        1783, 1921, 1927, 1929, 4286, 4456, 4687,
        4458, 4459, 4460, 4461, 4462, 4463, 4464, 4465,
        4466, 4467, 4468, 4469, 4470, 4471, 4472, 4473,
        4474, 4475, 4476, 4477, 4478, 4479, 4480, 4481, 4482,
    )

    override fun onThirdClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        when {
            itemId == 1921 || itemId == 4456 -> {
                client.deleteItem(itemId, itemSlot, 1)
                client.addItemSlot(1923, 1, itemSlot)
                client.checkItemUpdate()
                return true
            }

            itemId in 4458..4482 -> {
                client.deleteItem(itemId, itemSlot, 1)
                client.addItemSlot(1980, 1, itemSlot)
                client.checkItemUpdate()
                return true
            }

            itemId in intArrayOf(1783, 1927, 1929, 4286, 4687) -> {
                client.deleteItem(itemId, itemSlot, 1)
                client.addItemSlot(1925, 1, itemSlot)
                client.checkItemUpdate()
                if (itemId == 1927) {
                    client.requestAnim(0x33D, 0)
                    client.send(SendMessage("You drank the milk and gained 15% magic penetration!"))
                }
                return true
            }
        }
        return false
    }
}

