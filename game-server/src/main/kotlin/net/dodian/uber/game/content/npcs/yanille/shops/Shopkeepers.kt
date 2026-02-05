package net.dodian.uber.game.content.npcs.yanille.shops

import net.dodian.uber.game.content.npcs.NpcContent
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

object Shopkeepers : NpcContent {
    override val npcIds: IntArray = intArrayOf(506, 527)

    override fun onFirstClick(client: Client, npc: Npc, npcIndex: Int): Boolean {
        client.WanneShop = 3
        return true
    }

    override fun onSecondClick(client: Client, npc: Npc, npcIndex: Int): Boolean {
        client.WanneShop = 3
        return true
    }
}
