package net.dodian.uber.game.content.items

import net.dodian.uber.game.model.entity.player.Client

interface ItemContent {
    val itemIds: IntArray

    fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean = false
    fun onSecondClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean = false
    fun onThirdClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean = false
}

