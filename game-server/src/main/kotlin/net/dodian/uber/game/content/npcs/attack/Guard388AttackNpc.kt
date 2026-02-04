package net.dodian.uber.game.content.npcs.attack

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client



object Guard388AttackNpc : NpcAttackContent {
    override val npcIds: IntArray = intArrayOf(388)

    override fun onAttack(client: Client, npc: Npc, npcIndex: Int): Boolean {
        NpcAttackDispatcher.attackLikeLegacy(client, npc, npcIndex)
        return true
    }
}

