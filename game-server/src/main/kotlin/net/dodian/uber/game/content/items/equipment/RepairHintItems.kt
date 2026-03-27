package net.dodian.uber.game.content.items.equipment

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object RepairHintItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(4864, 4936)

    override fun onSecondClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        when (itemId) {
            4936 -> client.send(SendMessage("This crossbow need a Seercull bow to be fully repaired."))
            4864 -> client.send(SendMessage("This staff need a Master wand to be fully repaired."))
            else -> return false
        }
        return true
    }
}

