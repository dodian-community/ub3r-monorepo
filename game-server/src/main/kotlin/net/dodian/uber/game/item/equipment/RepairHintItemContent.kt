package net.dodian.uber.game.item.equipment

import net.dodian.uber.game.item.ItemContent
import net.dodian.uber.game.model.entity.player.Client

object RepairHintItemContent : ItemContent {
    override val itemIds: IntArray = intArrayOf(4864, 4936)

    override fun onSecondClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        when (itemId) {
            4936 -> client.sendMessage("This crossbow need a Seercull bow to be fully repaired.")
            4864 -> client.sendMessage("This staff need a Master wand to be fully repaired.")
            else -> return false
        }
        return true
    }
}


