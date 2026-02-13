package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueUi
import net.dodian.uber.game.model.player.skills.Skills
import java.text.NumberFormat

/**
 * Handles max cape purchase flow.
 * - Dialogue IDs: 6481, 6482, 6483, 6484
 * - NPC IDs: 6481
 */
object MaxCapeDialogueModule : DialogueRenderModule {

    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Misc.MAX_CAPE_GREETING) { c ->
            if (c.totalLevel() >= Skills.maxTotalLevel()) {
                c.showNPCChat(c.NpcTalkTo, 591, arrayOf("I see that you have trained up all your skills.", "I am utmost impressed!"))
            } else {
                c.showNPCChat(c.NpcTalkTo, 591, arrayOf("You are quite weak!"))
            }
            c.nextDiag = c.NpcDialogue + 1
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Misc.MAX_CAPE_OFFER) { c ->
            if (c.totalLevel() >= Skills.maxTotalLevel()) {
                c.showNPCChat(c.NpcTalkTo, 591, arrayOf("Would you like to purchase this cape on my back?", "It will cost you 13.37 million coins."))
                c.nextDiag = c.NpcDialogue + 1
            } else {
                c.showNPCChat(c.NpcTalkTo, 591, arrayOf("Come back when you have trained up your skills!"))
            }
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Misc.MAX_CAPE_CONFIRM) { c ->
            DialogueUi.showPlayerOption(c, arrayOf("Purchase the max cape?", "Yes", "No"))
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Misc.MAX_CAPE_PURCHASE) { c ->
            val coins = 13370000
            val freeSlot = if (c.getInvAmt(995) == coins) 1 else 2
            if (c.freeSlots() < freeSlot) {
                c.showNPCChat(c.NpcTalkTo, 591, arrayOf("You need atleast ${if (freeSlot == 1) "one" else "two"} free inventory slot${if (freeSlot != 1) "s" else ""}."))
                c.nextDiag = c.NpcTalkTo
            } else if (!c.playerHasItem(995, coins)) {
                c.showNPCChat(c.NpcTalkTo, 591, arrayOf("You are missing ${NumberFormat.getNumberInstance().format(coins - c.getInvAmt(995))} coins!"))
            } else {
                c.showNPCChat(c.NpcTalkTo, 591, arrayOf("Here you go.", "Max cape just for you."))
                c.deleteItem(995, coins)
                c.addItem(13281, 1)
                c.addItem(13280, 1)
                c.checkItemUpdate()
            }
            c.NpcDialogueSend = true
            true
        }
    }
}
