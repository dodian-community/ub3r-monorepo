package net.dodian.uber.game.content.items.runecrafting

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object RunePouchItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(
        5508, 5509, 5510, 5511, 5512, 5513, 5514, 5515,
    )

    override fun onSecondClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        val pouchSlot = if (itemId == 5509) 0 else ((itemId - 5508) / 2)
        if (pouchSlot in 0..3) {
            client.send(SendMessage("There is ${client.runePouchesAmount[pouchSlot]} rune essence in this pouch!"))
            return true
        }
        return false
    }
}

