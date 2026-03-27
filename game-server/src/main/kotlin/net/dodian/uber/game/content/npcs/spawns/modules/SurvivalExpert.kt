package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler

internal object SurvivalExpert {
    val npcIds: IntArray = intArrayOf(943)

    @Suppress("UNUSED_PARAMETER")
    fun onFirstClick(client: Client, npc: Npc): Boolean {
        var count = 0
        for (player in PlayerHandler.players) {
            if (player != null && player.wildyLevel > 0) {
                count++
            }
        }
        npc.setText("There are currently $count people in the wilderness")
        return true
    }
}
