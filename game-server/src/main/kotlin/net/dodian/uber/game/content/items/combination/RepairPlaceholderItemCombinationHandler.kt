package net.dodian.uber.game.content.items.combination

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object RepairPlaceholderItemCombinationHandler {
    @JvmStatic
    fun handle(client: Client, itemUsed: Int, useWith: Int): Boolean {
        var matched = false

        if ((itemUsed == 4938 && useWith == 4212) || (itemUsed == 4212 && useWith == 4938)) {
            client.send(SendMessage("WIP: Repair!"))
            matched = true
        } else if ((itemUsed == 4936 && useWith == 6724) || (itemUsed == 6724 && useWith == 4936)) {
            client.send(SendMessage("WIP: Repair!"))
            matched = true
        } else if (itemUsed == 4938 || useWith == 4938) {
            client.send(SendMessage("You need a Crystal bow to repair the Karil crossbow half way."))
            matched = true
        } else if (itemUsed == 4936 || useWith == 4936) {
            client.send(SendMessage("You need a Seercull bow to repair the Karil crossbow into full strength."))
            matched = true
        }

        if ((itemUsed == 4866 && useWith == 4675) || (itemUsed == 4675 && useWith == 4866)) {
            client.send(SendMessage("WIP: Repair!"))
            matched = true
        } else if ((itemUsed == 4864 && useWith == 6914) || (itemUsed == 6914 && useWith == 4864)) {
            client.send(SendMessage("WIP: Repair!"))
            matched = true
        } else if (itemUsed == 4866 || useWith == 4866) {
            client.send(SendMessage("You need a Ancient staff to repair the Ahrim staff half way."))
            matched = true
        } else if (itemUsed == 4864 || useWith == 4864) {
            client.send(SendMessage("You need a Master wand to repair the Ahrim staff into full strength."))
            matched = true
        }

        if (matched) {
            client.checkItemUpdate()
        }
        return matched
    }
}
