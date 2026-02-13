package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.dialogue

/**
 * Handles:
 * - Dialogue IDs: 2180, 2181, 2182, 2345, 2346, 2347, 48054
 * - NPC IDs: 2180, 2345
 */
object DungeonAccessDialogueModule : DialogueRenderModule {

    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Dungeon.DUNGEON_ENTRY_GREETING) { c ->
            if (!c.checkUnlock(0)) {
                c.showNPCChat(c.NpcTalkTo, 591, arrayOf("Hello!", "Are you looking to enter my dungeon?", "You have to pay a to enter.", "You can also pay a one time fee."))
                c.NpcDialogue += 1
                c.nextDiag = c.NpcDialogue
                c.NpcDialogueSend = true
                true
            } else {
                dialogue(c) {
                    npcChat(c.NpcTalkTo, 591, "You can enter freely, no need to pay me anything.")
                }
            }
        }

        builder.handle(DialogueIds.Dungeon.DUNGEON_ENTRY_OPTIONS) { c ->
            if (!c.checkUnlock(0) && c.checkUnlockPaid(0) != 1) {
                dialogue(c) {
                    options("Select a option", "Enter fee", "Permanent unlock", "Nevermind")
                }
            } else if (c.checkUnlock(0)) {
                dialogue(c) {
                    npcChat(c.NpcTalkTo, 591, "You can enter freely, no need to pay me anything.")
                }
            } else {
                dialogue(c) {
                    npcChat(c.NpcTalkTo, 591, "You have already paid.", "Just enter the dungeon now.")
                }
            }
        }

        builder.handle(DialogueIds.Dungeon.DUNGEON_PAYMENT_OPTIONS) { c ->
            dialogue(c) {
                options("Select a option", "Ship ticket", "Coins")
            }
        }

        builder.handle(DialogueIds.Dungeon.CAVE_ENTRY_GREETING) { c ->
            if (!c.checkUnlock(1)) {
                c.showNPCChat(c.NpcTalkTo, 591, arrayOf("Hello!", "Are you looking to enter my cave?", "You have to pay a to enter.", "You can also pay a one time fee."))
                c.NpcDialogue += 1
                c.nextDiag = c.NpcDialogue
                c.NpcDialogueSend = true
                true
            } else {
                dialogue(c) {
                    npcChat(c.NpcTalkTo, 591, "You can enter freely, no need to pay me anything.")
                }
            }
        }

        builder.handle(DialogueIds.Dungeon.CAVE_ENTRY_OPTIONS) { c ->
            if (!c.checkUnlock(1) && c.checkUnlockPaid(1) != 1) {
                dialogue(c) {
                    options("Select a option", "Enter fee", "Permanent unlock", "Nevermind")
                }
            } else if (c.checkUnlock(1)) {
                dialogue(c) {
                    npcChat(c.NpcTalkTo, 591, "You can enter freely, no need to pay me anything.")
                }
            } else {
                dialogue(c) {
                    npcChat(c.NpcTalkTo, 591, "You have already paid.", "Just enter the cave now.")
                }
            }
        }

        builder.handle(DialogueIds.Dungeon.CAVE_PAYMENT_OPTIONS) { c ->
            dialogue(c) {
                options("Select a option", "Ship ticket", "Coins")
            }
        }

        builder.handle(DialogueIds.Dungeon.TRAVEL_UNLOCK) { c ->
            dialogue(c) {
                options("Unlock the travel?", "Yes", "No")
            }
        }
    }
}
