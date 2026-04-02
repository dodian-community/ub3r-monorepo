package net.dodian.uber.game.content.npcs

import net.dodian.uber.game.systems.ui.dialogue.DialogueEmote
import net.dodian.uber.game.systems.ui.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object Monk : NpcModule {
    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 555, x = 2604, y = 3092, z = 0, face = 0),
    )
    val npcIds: IntArray = npcIdsFromEntries(entries)


    override val definition = legacyNpcDefinition(
        name = "Monk",
        entries = entries,
        onFirstClick = ::onFirstClick,
    )

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        DialogueService.start(client) {
            npcChat(npc.id, DialogueEmote.DEFAULT, "Peace be with you.")
            finish()
        }
        return true
    }
}
