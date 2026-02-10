package net.dodian.uber.game.content.npcs.dialogue.modules

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.dialogue
import net.dodian.uber.game.party.RewardItem
import net.dodian.utilities.Utils

/**
 * Handles:
 * - Dialogue IDs: 1174..1178, 4753..4759
 * - NPC IDs: 1174, 4753
 */
object HerbloreDialogueModule : DialogueRenderModule {

    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Herblore.DECANT_GREETING) { c ->
            dialogue(c) {
                npcChat(c.NpcTalkTo, 591, "Hello there adventurer ${c.playerName},", "is there anything you are looking for?")
                setNextDiag(DialogueIds.Herblore.DECANT_PLAYER_QUERY)
            }
        }

        builder.handle(DialogueIds.Herblore.DECANT_PLAYER_QUERY) { c ->
            dialogue(c) {
                playerChat(614, "Is there anything you can do for me?", "I heard you know alot about herblore.")
                setNextDiag(DialogueIds.Herblore.DECANT_INFO)
            }
        }

        builder.handle(DialogueIds.Herblore.DECANT_INFO) { c ->
            dialogue(c) {
                npcChat(
                    c.NpcTalkTo,
                    591,
                    "For you ${c.playerName} I know the art of decanting.",
                    "I can offer you to decant any noted potions",
                    "that you will bring me, free of charge.",
                    "I also got a herblore store if you wish to take a look."
                )
                setNextDiag(DialogueIds.Herblore.DECANT_OPTIONS)
            }
        }

        builder.handle(DialogueIds.Herblore.DECANT_OPTIONS) { c ->
            dialogue(c) {
                options("What do you wish to do?", "Visit store", "Decant potions", "Nevermind")
            }
        }

        builder.handle(DialogueIds.Herblore.DECANT_DOSING_OPTIONS) { c ->
            dialogue(c) {
                options("What should we decant it into?", "Four dose", "Three dose", "Two dose", "One dose", "Nevermind")
            }
        }

        builder.handle(DialogueIds.Herblore.HERBALIST_GREETING) { c ->
            dialogue(c) {
                npcChat(c.NpcTalkTo, 591, "Hello ${if (c.gender == 1) "miss" else "mr"} adventurer.", "What can I help you with today?")
                setNextDiag(DialogueIds.Herblore.HERBALIST_PLAYER_QUERY)
            }
        }

        builder.handle(DialogueIds.Herblore.HERBALIST_PLAYER_QUERY) { c ->
            dialogue(c) {
                playerChat(612, "I heard you were a famous herbalist.", "I was wondering if you had some kind of service.")
                setNextDiag(DialogueIds.Herblore.HERBALIST_INFO)
            }
        }

        builder.handle(DialogueIds.Herblore.HERBALIST_INFO) { c ->
            dialogue(c) {
                npcChat(c.NpcTalkTo, 591, "I sure do have some services I can offer.", "Would you like me to make you a unfinish potion or", "Clean any of your herbs? They must all be noted.")
                setNextDiag(DialogueIds.Herblore.HERBALIST_COST_INFO)
            }
        }

        builder.handle(DialogueIds.Herblore.HERB_CLEANER) { c ->
            c.herbMaking = 0
            c.herbOptions.clear()
            for (h in Utils.grimy_herbs.indices) {
                if (c.playerHasItem(c.GetNotedItem(Utils.grimy_herbs[h]))) {
                    c.herbOptions.add(RewardItem(c.GetNotedItem(Utils.grimy_herbs[h]), 0))
                }
            }
            if (c.herbOptions.isEmpty()) {
                c.showNPCChat(4753, 605, arrayOf("You got no herbs for me to clean!"))
            } else {
                c.setHerbOptions()
            }
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Herblore.UNF_POTION_MAKER) { c ->
            c.herbMaking = 0
            c.herbOptions.clear()
            for (h in Utils.herbs.indices) {
                if (c.playerHasItem(c.GetNotedItem(Utils.herbs[h]))) {
                    c.herbOptions.add(RewardItem(c.GetNotedItem(Utils.herb_unf[h]), 0))
                }
            }
            if (c.herbOptions.isEmpty()) {
                c.showNPCChat(4753, 605, arrayOf("You got no herbs for me to make into unfinish potions!"))
            } else {
                c.setHerbOptions()
            }
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Herblore.HERBALIST_COST_INFO) { c ->
            dialogue(c) {
                npcChat(c.NpcTalkTo, 591, "This service will cost you 200 coins per herb", "and 1000 coins per potion.", "I also got a nice store if you wish to take a look.")
                setNextDiag(DialogueIds.Herblore.HERBALIST_OPTIONS)
            }
        }

        builder.handle(DialogueIds.Herblore.HERBALIST_OPTIONS) { c ->
            dialogue(c) {
                options("Select a option", "Visit the store", "Clean herbs", "Make unfinish potions")
            }
        }
    }
}
