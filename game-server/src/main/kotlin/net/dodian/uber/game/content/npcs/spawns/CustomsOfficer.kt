package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object CustomsOfficer {
    // Stats: 3648: r=0 a=0 d=0 s=0 hp=0 rg=0 mg=0

    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 3648, x = 3274, y = 2797, z = 0, face = 0),
        NpcSpawnDef(npcId = 3648, x = 3511, y = 3505, z = 0, face = 0),
        NpcSpawnDef(npcId = 3648, x = 2772, y = 3235, z = 0, face = 4),
        NpcSpawnDef(npcId = 3648, x = 2804, y = 3421, z = 0, face = 6),
        NpcSpawnDef(npcId = 3648, x = 2864, y = 2971, z = 0, face = 6),
    )

    val npcIds: IntArray = entries.map { it.npcId }.distinct().toIntArray()

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        DialogueService.start(client) {
            npcChat(npc.id, DialogueEmote.DEFAULT, "Hello dear.", "Would you like to travel?")
            options(
                title = "Do you wish to travel?",
                DialogueOption("Yes") {
                    action { c -> c.setTravelMenu() }
                    finish(closeInterfaces = false)
                },
                DialogueOption("No") {
                    playerChat(DialogueEmote.ANGRY1, "No thank you.")
                    finish()
                },
            )
        }
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSecondClick(client: Client, npc: Npc): Boolean {
        client.setTravelMenu()
        return true
    }
}
