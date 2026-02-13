package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object PartyPete {
    val npcIds: IntArray = intArrayOf(659)

    @Suppress("UNUSED_PARAMETER")
    fun onFirstClick(client: Client, npc: Npc): Boolean {
        client.NpcWanneTalk = 1000
        client.convoId = 1001
        return true
    }
}
