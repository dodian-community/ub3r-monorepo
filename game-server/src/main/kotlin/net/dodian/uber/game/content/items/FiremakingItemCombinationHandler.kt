package net.dodian.uber.game.content.items

import net.dodian.uber.game.content.skills.firemaking.Firemaking
import net.dodian.uber.game.model.entity.player.Client

object FiremakingItemCombinationHandler {
    @JvmStatic
    fun handle(client: Client, itemUsed: Int, useWith: Int): Boolean =
        Firemaking.handleItemCombination(client, itemUsed, useWith)
}
