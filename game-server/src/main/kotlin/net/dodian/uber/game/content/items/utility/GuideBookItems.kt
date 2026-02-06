package net.dodian.uber.game.content.items.utility

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client

object GuideBookItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(1856)

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        client.guideBook()
        return true
    }
}
