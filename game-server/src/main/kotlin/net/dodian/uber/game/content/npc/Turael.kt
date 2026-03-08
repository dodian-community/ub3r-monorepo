package net.dodian.uber.game.content.npc

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object Turael {
    val npcIds: IntArray = intArrayOf(70)

    @Suppress("UNUSED_PARAMETER")
    fun onThirdClick(client: Client, npc: Npc): Boolean {
        client.openUpShopRouted(2)
        return true
    }
}
