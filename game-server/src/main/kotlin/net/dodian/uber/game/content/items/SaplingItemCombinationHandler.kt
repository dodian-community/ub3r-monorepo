package net.dodian.uber.game.content.items

import net.dodian.uber.game.model.entity.player.Client

object SaplingItemCombinationHandler {
    @JvmStatic
    fun handle(client: Client, useWith: Int, usedWithSlot: Int, itemUsed: Int, itemUsedSlot: Int) {
        with(client.farming) {
            client.saplingMaking(useWith, usedWithSlot, itemUsed, itemUsedSlot)
        }
    }
}
