package net.dodian.uber.game.item.reward

import net.dodian.uber.game.item.ItemContent
import net.dodian.uber.game.model.entity.player.Client

object LampRewardItemContent : ItemContent {
    override val itemIds: IntArray = intArrayOf(
        2528, // Genie lamp
        6543, // Antique lamp
    )

    override fun onFirstClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        when (itemId) {
            2528 -> client.openGenie()
            6543 -> client.openAntique()
            else -> return false
        }
        return true
    }
}

