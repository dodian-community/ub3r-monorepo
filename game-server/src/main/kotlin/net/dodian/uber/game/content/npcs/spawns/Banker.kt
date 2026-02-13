package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object Banker {
    val npcIds: IntArray = intArrayOf(394, 395, 7677)

    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 394, x = 2727, y = 3378, z = 0, face = 0),
    )

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        DialogueService.start(client) {
            npcChat(npc.id, DialogueEmote.DEFAULT, "Good day, how can I help you?")
            options(
                title = "What would you like to say?",
                DialogueOption("I'd like to access my bank account, please.") {
                    action { c -> c.openUpBank() }
                    finish(closeInterfaces = false)
                },
                DialogueOption("I'd like to check my PIN settings.") {
                    npcChat(npc.id, DialogueEmote.DEFAULT, "Pins have not been implemented yet.")
                    finish()
                },
            )
        }
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSecondClick(client: Client, npc: Npc): Boolean {
        client.WanneBank = 1
        return true
    }
}
