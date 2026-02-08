package net.dodian.uber.game.content.npcs.yanille

import net.dodian.uber.game.content.npcs.NpcContent
import net.dodian.uber.game.content.npcs.NpcDispatcher
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

object BattleMage : NpcContent {
    override val npcIds: IntArray = intArrayOf(162)

    override fun onAttack(client: Client, npc: Npc, npcIndex: Int): Boolean {
        NpcDispatcher.attackLikeLegacy(client, npc, npcIndex)
        return true
    }
}
