package net.dodian.uber.game.npc

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.interaction.npcs.NpcInteractionActionService
import net.dodian.uber.game.shop.ShopId

internal object ShopAssistant {
    val npcIds: IntArray = intArrayOf(527)

    @Suppress("UNUSED_PARAMETER")
    fun onSecondClick(client: Client, npc: Npc): Boolean {
        NpcInteractionActionService.openShop(client, ShopId.GENERAL_STORE)
        return true
    }
}
