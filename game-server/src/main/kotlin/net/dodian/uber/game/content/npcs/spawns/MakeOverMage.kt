package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SetTabInterface

internal object MakeoverMage {
    val npcIds: IntArray = intArrayOf(1306)

    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 1306, x = 2603, y = 3088, z = 0, face = 0),
    )

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        DialogueService.start(client) {
            npcChat(
                npc.id,
                DialogueEmote.DEFAULT,
                "Hello there, would you like to change your looks? If so, it will be free of charge.",
            )
            options(
                title = "Would you like to change your looks?",
                DialogueOption("Sure") {
                    playerChat(DialogueEmote.DEFAULT, "I would love that.")
                    action { c -> c.send(SetTabInterface(3559, 3213)) }
                    finish(closeInterfaces = false)
                },
                DialogueOption("No thanks") {
                    playerChat(DialogueEmote.DEFAULT, "Not at the moment.")
                    finish()
                },
            )
        }
        return true
    }
}
