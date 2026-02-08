package net.dodian.uber.game.content.npcs.yanille

import net.dodian.uber.game.content.npcs.NpcContent
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

object MakeoverMage : NpcContent {
    override val npcIds: IntArray = intArrayOf(1306, 1307)

    override fun onFirstClick(client: Client, npc: Npc, npcIndex: Int): Boolean {
        client.NpcWanneTalk = 21
        return true
    }

    override fun onThirdClick(client: Client, npc: Npc, npcIndex: Int): Boolean {
        client.NpcWanneTalk = 23
        return true
    }
}
