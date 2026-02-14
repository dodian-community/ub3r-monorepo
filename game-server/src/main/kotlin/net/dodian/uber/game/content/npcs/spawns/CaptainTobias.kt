package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object CaptainTobias {
    val npcIds: IntArray = intArrayOf(376)

    @Suppress("UNUSED_PARAMETER")
    fun onFirstClick(client: Client, npc: Npc): Boolean {
        if (client.playerRights == 2) {
            client.triggerTele(2772, 3234, 0, false)
        }
        return true
    }
}
