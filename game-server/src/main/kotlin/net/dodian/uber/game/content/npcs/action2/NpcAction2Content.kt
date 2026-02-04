package net.dodian.uber.game.content.npcs.action2

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

interface NpcAction2Content {
    val npcIds: IntArray

    /**
     * Return true if handled (skip legacy click2 logic).
     */
    fun onClick2(client: Client, npc: Npc, npcIndex: Int): Boolean = false
}

