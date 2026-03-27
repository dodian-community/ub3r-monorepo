package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.skills.agility.AgilityPlugin

internal object AgilityWerewolfMaster {
    val npcIds: IntArray = intArrayOf(5927)

    @Suppress("UNUSED_PARAMETER")
    fun onFirstClick(client: Client, npc: Npc): Boolean {
        AgilityPlugin.werewolf(client).handStick()
        return true
    }
}
