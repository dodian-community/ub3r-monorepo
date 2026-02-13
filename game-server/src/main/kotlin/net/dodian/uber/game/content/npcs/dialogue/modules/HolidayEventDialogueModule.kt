package net.dodian.uber.game.content.npcs.dialogue.modules

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueUi

/**
 * Handles holiday event intro flow.
 * - Dialogue IDs: 8051, 8052, 8053
 * - NPC IDs: 8051
 */
object HolidayEventDialogueModule : DialogueRenderModule {

    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Misc.HOLIDAY_GREETING) { c ->
            c.showNPCChat(c.NpcTalkTo, 591, arrayOf("Happy Holidays adventurer!"))
            c.nextDiag = DialogueIds.Misc.HOLIDAY_INFO
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Misc.HOLIDAY_INFO) { c ->
            c.showNPCChat(c.NpcTalkTo, 591, arrayOf("The monsters are trying to ruin the new year!", "You must slay them to take back your gifts and", "save the spirit of 2021!"))
            c.nextDiag = DialogueIds.Misc.HOLIDAY_OPTIONS
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Misc.HOLIDAY_OPTIONS) { c ->
            DialogueUi.showPlayerOption(c, arrayOf("Select a option", "I'd like to see your shop.", "I'll just be on my way."))
            c.NpcDialogueSend = true
            true
        }
    }
}
