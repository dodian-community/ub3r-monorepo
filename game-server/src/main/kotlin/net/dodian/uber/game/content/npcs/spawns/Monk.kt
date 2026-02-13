package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object Monk {
    val npcIds: IntArray = intArrayOf(555)

    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 555, x = 2604, y = 3092, z = 0, face = 0),
    )

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        DialogueService.start(client) {
            npcChat(npc.id, DialogueEmote.DEFAULT, "Peace be with you.")
            finish()
        }
        return true
    }
}
