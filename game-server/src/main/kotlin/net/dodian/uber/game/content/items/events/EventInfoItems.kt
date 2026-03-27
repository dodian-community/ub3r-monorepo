package net.dodian.uber.game.content.items.events

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object EventInfoItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(11997)

    override fun onSecondClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        client.send(SendMessage("Event is over! Will use in the future?!"))
        return true
    }
}

