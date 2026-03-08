package net.dodian.uber.game.content.npc

import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.dialogue.core.DialogueIds
import net.dodian.uber.game.content.dialogue.core.DialogueRenderRegistry
import net.dodian.uber.game.content.dialogue.core.DialogueUi
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object DukeHoracio {
    val npcIds: IntArray = intArrayOf(8051)

    @Suppress("UNUSED_PARAMETER")
    fun onFirstClick(client: Client, npc: Npc): Boolean {
        client.startNpcDialogue(8051, npc.id)
        return true
    }

    fun registerDialogueRenders(builder: DialogueRenderRegistry.Builder) {
        builder.handle(DialogueIds.Misc.HOLIDAY_GREETING) { c ->
            DialogueService.showNpcChat(c, DialogueService.activeNpcId(c), 591, arrayOf("Happy Holidays adventurer!"), DialogueIds.Misc.HOLIDAY_INFO)
            true
        }

        builder.handle(DialogueIds.Misc.HOLIDAY_INFO) { c ->
            DialogueService.showNpcChat(
                c,
                DialogueService.activeNpcId(c),
                591,
                arrayOf("The monsters are trying to ruin the new year!", "You must slay them to take back your gifts and", "save the spirit of 2021!"),
                DialogueIds.Misc.HOLIDAY_OPTIONS,
            )
            true
        }

        builder.handle(DialogueIds.Misc.HOLIDAY_OPTIONS) { c ->
            DialogueUi.showPlayerOption(c, arrayOf("Select a option", "I'd like to see your shop.", "I'll just be on my way."))
            DialogueService.setDialogueSent(c, true)
            true
        }
    }
}
