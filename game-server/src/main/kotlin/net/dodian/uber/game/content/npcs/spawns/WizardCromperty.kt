package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object WizardCromperty {
    val npcIds: IntArray = intArrayOf(844)

    @Suppress("UNUSED_PARAMETER")
    fun onSecondClick(client: Client, npc: Npc): Boolean {
        client.stairs = 26
        client.stairDistance = 1
        return true
    }
}
