package net.dodian.uber.game.content.npcs

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.content.npcs.NpcInteractionActionService

internal object ShopAssistant {
    val npcIds: IntArray = intArrayOf(527)

    @Suppress("UNUSED_PARAMETER")
    fun onSecondClick(client: Client, npc: Npc): Boolean {
        NpcInteractionActionService.openShop(client, 3)
        return true
    }
}
