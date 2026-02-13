package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueUi
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
}

object BoatTravelDialogueModule : DialogueRenderModule {

    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Misc.BOAT_GREETING) { c ->
            c.showNPCChat(c.NpcTalkTo, 591, arrayOf("Hello dear.", "Would you like to travel?"))
            c.nextDiag = DialogueIds.Misc.BOAT_OPTIONS
            true
        }

        builder.handle(DialogueIds.Misc.BOAT_OPTIONS) { c ->
            DialogueUi.showPlayerOption(c, arrayOf("Do you wish to travel?", "Yes", "No"))
            true
        }
    }
}
