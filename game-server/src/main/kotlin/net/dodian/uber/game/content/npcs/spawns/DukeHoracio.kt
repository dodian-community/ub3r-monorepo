package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.dialogue.legacy.core.DialogueIds
import net.dodian.uber.game.content.dialogue.legacy.core.DialogueRegistry
import net.dodian.uber.game.content.dialogue.legacy.core.DialogueUi
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object DukeHoracio {
    val npcIds: IntArray = intArrayOf(8051)

    @Suppress("UNUSED_PARAMETER")
    fun onFirstClick(client: Client, npc: Npc): Boolean {
        client.NpcWanneTalk = 8051
        return true
    }

    fun registerLegacyDialogues(builder: DialogueRegistry.Builder) {
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
