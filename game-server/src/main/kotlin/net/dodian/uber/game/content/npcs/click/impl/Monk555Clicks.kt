package net.dodian.uber.game.content.npcs.click.impl

import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.npcs.click.NpcClickContent
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

object Monk555Clicks : NpcClickContent {
    override val npcIds: IntArray = intArrayOf(555)

    override fun onFirstClick(client: Client, npc: Npc): Boolean {
        DialogueService.start(client) {
            npcChat(555, 591, "Suddenly the monk had an urge to disappear!")
            finish()
        }
        return true
    }
}

