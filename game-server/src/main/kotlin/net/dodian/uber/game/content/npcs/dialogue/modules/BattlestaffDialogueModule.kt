package net.dodian.uber.game.content.npcs.dialogue.modules

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueUi

/**
 * Handles daily battlestaff claim flow.
 * - Dialogue IDs: 3837, 3838
 * - NPC IDs: 3837
 */
object BattlestaffDialogueModule : DialogueRenderModule {

    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Misc.BATTLESTAFF_GREETING) { c ->
            val staffCounting = if (c.dailyReward.isEmpty()) 0 else Integer.parseInt(c.dailyReward[2])
            if (staffCounting > 0) {
                c.showNPCChat(c.NpcTalkTo, 594, arrayOf("Fancy meeting you here mysterious one.", "I have $staffCounting battlestaffs that", "you can claim for 7000 coins each."))
                c.nextDiag = DialogueIds.Misc.BATTLESTAFF_OPTIONS
            } else {
                c.showNPCChat(c.NpcTalkTo, 597, arrayOf("Fancy meeting you here mysterious one.", "I have no battlestaffs that you can claim."))
            }
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Misc.BATTLESTAFF_OPTIONS) { c ->
            DialogueUi.showPlayerOption(c, arrayOf("Do you wish to claim your battlestaffs?", "Yes", "No"))
            c.NpcDialogueSend = true
            true
        }
    }
}
