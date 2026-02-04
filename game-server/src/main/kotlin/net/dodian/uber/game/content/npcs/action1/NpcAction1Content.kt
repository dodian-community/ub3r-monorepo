package net.dodian.uber.game.content.npcs.action1

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

interface NpcAction1Content {
    val npcIds: IntArray

    /**
     * Return true if handled (skip legacy click1 logic).
     */
    fun onClick1(client: Client, npc: Npc, npcIndex: Int): Boolean = false
}

