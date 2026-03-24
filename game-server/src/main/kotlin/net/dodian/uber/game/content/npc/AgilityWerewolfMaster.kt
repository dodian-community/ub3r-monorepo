package net.dodian.uber.game.content.npc

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.agility.AgilityDefinitions
import net.dodian.uber.game.skills.agility.WerewolfCourseService

internal object AgilityWerewolfMaster {
    val npcIds: IntArray = intArrayOf(AgilityDefinitions.WEREWOLF_MASTER_NPC_ID)

    @Suppress("UNUSED_PARAMETER")
    fun onFirstClick(client: Client, npc: Npc): Boolean {
        WerewolfCourseService(client).handStick()
        return true
    }
}
