package net.dodian.uber.game.item.reward

import net.dodian.uber.game.item.ItemContent
import net.dodian.uber.game.model.entity.player.Client

object EventInfoItemContent : ItemContent {
    override val itemIds: IntArray = intArrayOf(11997)

    override fun onSecondClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        client.sendMessage("Event is over! Will use in the future?!")
        return true
    }
}


