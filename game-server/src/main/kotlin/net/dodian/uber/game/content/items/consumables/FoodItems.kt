package net.dodian.uber.game.content.items.consumables

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client

object FoodItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(
        315, 2140, 2142, 2309, 3369, 333, 329, 379, 373, 7946, 385, 397, 391, 1959, 1961,
    )

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        val healAmount = when (itemId) {
            315, 2140, 2142 -> 3
            2309 -> 5
            3369 -> 7
            333 -> 8
            329 -> 10
            379 -> 12
            373 -> 14
            7946 -> 16
            385 -> 20
            397 -> 22
            391 -> 24
            1959, 1961 -> 2
            else -> return false
        }
        client.eat(healAmount, itemId, itemSlot)
        return true
    }
}
