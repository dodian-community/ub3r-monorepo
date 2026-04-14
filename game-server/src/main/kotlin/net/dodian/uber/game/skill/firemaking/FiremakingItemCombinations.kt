package net.dodian.uber.game.skill.firemaking

import net.dodian.uber.game.model.entity.player.Client

object FiremakingItemCombinations {
    @JvmStatic
    fun handle(client: Client, itemUsed: Int, useWith: Int): Boolean =
        Firemaking.handleItemCombination(client, itemUsed, useWith)
}
