package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueUi

/**
 * Handles boat travel flow.
 * - Dialogue IDs: 3648, 3649
 * - NPC IDs: 3648
 */
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
