package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.agility.Werewolf

internal object AgilityWerewolfMaster {
    val npcIds: IntArray = intArrayOf(5927)

    @Suppress("UNUSED_PARAMETER")
    fun onFirstClick(client: Client, npc: Npc): Boolean {
        Werewolf(client).handStick()
        return true
    }
}
