package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object ShopAssistant {
    val npcIds: IntArray = intArrayOf(527)

    @Suppress("UNUSED_PARAMETER")
    fun onSecondClick(client: Client, npc: Npc): Boolean {
        client.WanneShop = 3
        return true
    }
}
