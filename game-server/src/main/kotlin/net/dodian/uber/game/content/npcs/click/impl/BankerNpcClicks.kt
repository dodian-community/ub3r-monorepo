package net.dodian.uber.game.content.npcs.click.impl

import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.npcs.click.NpcClickContent
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

object BankerNpcClicks : NpcClickContent {
    override val npcIds: IntArray = intArrayOf(394, 395, 7677)

    override fun onFirstClick(client: Client, npc: Npc): Boolean {
        val npcId = npc.id
        DialogueService.start(client) {
            npcChat(npcId, 591, "Good day, how can I help you?")
            options(
                title = "What would you like to say?",
                DialogueOption("I'd like to access my bank account, please.") {
                    action { c -> c.openUpBank() }
                    finish(closeInterfaces = false)
                },
                DialogueOption("I'd like to check my PIN settings.") {
                    npcChat(npcId, 591, "Pins have not been implemented yet.")
                    finish()
                },
            )
        }
        return true
    }
}

