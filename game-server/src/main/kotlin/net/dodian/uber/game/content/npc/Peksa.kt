package net.dodian.uber.game.content.npc

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object Peksa {
    val npcIds: IntArray = intArrayOf(538)

    @Suppress("UNUSED_PARAMETER")
    fun onSecondClick(client: Client, npc: Npc): Boolean {
        client.openUpShopRouted(6)
        return true
    }
}
