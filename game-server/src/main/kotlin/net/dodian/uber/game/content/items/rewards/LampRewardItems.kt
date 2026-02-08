package net.dodian.uber.game.content.items.rewards

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client

object LampRewardItems : ItemContent {
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
