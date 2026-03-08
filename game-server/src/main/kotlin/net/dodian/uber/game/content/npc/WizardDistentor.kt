package net.dodian.uber.game.content.npc

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.objects.travel.EssenceMineTravel

internal object WizardDistentor {
    val npcIds: IntArray = intArrayOf(462)

    @Suppress("UNUSED_PARAMETER")
    fun onSecondClick(client: Client, npc: Npc): Boolean = EssenceMineTravel.sendToEssenceMine(client)
}
