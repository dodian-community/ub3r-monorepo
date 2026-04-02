package net.dodian.uber.game.content.items.combination

import net.dodian.uber.game.content.skills.firemaking.Firemaking
import net.dodian.uber.game.model.entity.player.Client

object FiremakingItemCombinations {
    @JvmStatic
    fun handle(client: Client, itemUsed: Int, useWith: Int): Boolean =
        Firemaking.handleItemCombination(client, itemUsed, useWith)
}
