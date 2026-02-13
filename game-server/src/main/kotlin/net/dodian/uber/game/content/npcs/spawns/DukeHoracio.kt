package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object DukeHoracio {
    val npcIds: IntArray = intArrayOf(8051)

    @Suppress("UNUSED_PARAMETER")
    fun onFirstClick(client: Client, npc: Npc): Boolean {
        client.NpcWanneTalk = 8051
        return true
    }
}
