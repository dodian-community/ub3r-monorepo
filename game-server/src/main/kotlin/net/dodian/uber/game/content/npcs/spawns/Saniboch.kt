package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.dialogue
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object Saniboch {
    // Stats: 2345: r=60 a=0 d=0 s=0 hp=0 rg=0 mg=0

    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 2345, x = 2743, y = 3150, z = 0, face = 0),
    )

    val npcIds: IntArray = entries.map { it.npcId }.distinct().toIntArray()

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        DialogueService.start(client) {
            if (client.checkUnlock(0)) {
                npcChat(npc.id, DialogueEmote.DEFAULT, "You can enter freely, no need to pay me anything.")
                finish()
            } else {
                npcChat(npc.id, DialogueEmote.DEFAULT, "Hello!", "Are you looking to enter my dungeon?", "You have to pay to enter.", "You can also pay a one time fee.")
                options(
                    title = "Select an option",
                    DialogueOption("Enter fee") {
                        options(
                            title = "Select a payment option",
                            DialogueOption("Ship ticket") {
                                action { c ->
                                    if (c.checkUnlockPaid(0) > 0) {
                                        c.showNPCChat(npc.id, 591, arrayOf("You have already paid me.", "Please step into my dungeon."))
                                    } else if (c.getInvAmt(621) > 0 || c.getBankAmt(621) > 0) {
                                        c.addUnlocks(0, "1", if (c.checkUnlock(0)) "1" else "0")
                                        if (c.getInvAmt(621) > 0) c.deleteItem(621, 1) else c.deleteItemBank(621, 1)
                                        c.checkItemUpdate()
                                        c.showNPCChat(npc.id, 591, arrayOf("You can now step into the dungeon."))
                                    } else {
                                        c.showNPCChat(npc.id, 596, arrayOf("You need a ship ticket to enter my dungeon!"))
                                    }
                                }
                                finish()
                            },
                            DialogueOption("Coins") {
                                action { c ->
                                    val amount = c.getInvAmt(995).toLong() + c.getBankAmt(995)
                                    val total = 300_000
                                    if (amount >= total) {
                                        c.addUnlocks(0, "1", if (c.checkUnlock(0)) "1" else "0")
                                        val remain = total - c.getInvAmt(995)
                                        c.deleteItem(995, total)
                                        if (remain > 0) c.deleteItemBank(995, remain)
                                        c.showNPCChat(npc.id, 591, arrayOf("You can now step into the dungeon."))
                                    } else {
                                        c.showNPCChat(npc.id, 596, arrayOf("You need at least ${total - amount} more coins to enter my dungeon!"))
                                    }
                                }
                                finish()
                            },
                        )
                    },
                    DialogueOption("Permanent unlock") {
                        action { c ->
                            if (!c.checkUnlock(0)) {
                                val maximumTickets = 10
                                val minimumTicket = 1
                                val ticketValue = 300_000
                                var missing = (maximumTickets - minimumTicket) * ticketValue
                                if (!c.playerHasItem(621, minimumTicket)) {
                                    c.showNPCChat(npc.id, 591, arrayOf("You need a minimum of $minimumTicket ship ticket", "to unlock permanent!"))
                                } else {
                                    missing -= (c.getInvAmt(621) - minimumTicket) * ticketValue
                                    if (missing > 0) {
                                        if (c.getInvAmt(995) >= missing) {
                                            c.deleteItem(621, kotlin.math.min(c.getInvAmt(621), maximumTickets))
                                            c.deleteItem(995, missing)
                                            c.addUnlocks(0, c.checkUnlockPaid(0).toString(), "1")
                                            c.showNPCChat(npc.id, 591, arrayOf("Thank you for the payment.", "You may enter freely into my dungeon."))
                                        } else {
                                            c.showNPCChat(npc.id, 591, arrayOf("You do not have enough coins to do this!"))
                                        }
                                    } else {
                                        c.deleteItem(621, maximumTickets)
                                        c.addUnlocks(0, c.checkUnlockPaid(0).toString(), "1")
                                        c.showNPCChat(npc.id, 591, arrayOf("Thank you for the ship tickets.", "You may enter freely into my dungeon."))
                                    }
                                    c.checkItemUpdate()
                                }
                            }
                        }
                        finish()
                    },
                    DialogueOption("Nevermind") {
                        playerChat(DialogueEmote.DEFAULT, "I do not want anything.")
                        finish()
                    },
                )
            }
        }
        return true
    }
}

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
