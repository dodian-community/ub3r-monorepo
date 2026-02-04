package net.dodian.uber.game.content.npcs.attack

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

interface NpcAttackContent {
    val npcIds: IntArray


    fun onAttack(client: Client, npc: Npc, npcIndex: Int): Boolean = false
}

