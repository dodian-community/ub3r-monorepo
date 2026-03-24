package net.dodian.uber.game.content.npc

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.agility.WerewolfCourseService

internal object AgilityWerewolfMaster {
    val npcIds: IntArray = intArrayOf(5927)

    @Suppress("UNUSED_PARAMETER")
    fun onFirstClick(client: Client, npc: Npc): Boolean {
        WerewolfCourseService(client).handStick()
        return true
    }
}
