package net.dodian.uber.game.content.items.runecrafting

import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.runecrafting.RunecraftingPlugin

object RunePouchItems : ItemContent {
    override val itemIds: IntArray = intArrayOf(
        5508, 5509, 5510, 5511, 5512, 5513, 5514, 5515,
    )

    override fun onSecondClick(client: Client, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        return RunecraftingPlugin.checkPouch(client, itemId)
    }
}
