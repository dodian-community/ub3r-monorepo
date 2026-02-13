package net.dodian.uber.game.content.npcs.spawns

import java.text.NumberFormat
import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.netty.listener.out.SendMessage

internal object Mac {
    // Stats: 6481: r=60 a=0 d=0 s=0 hp=0 rg=0 mg=0

    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 6481, x = 2735, y = 3370, z = 0, face = 0),
    )

    val npcIds: IntArray = entries.map { it.npcId }.distinct().toIntArray()

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        DialogueService.start(client) {
            if (client.totalLevel() >= Skills.maxTotalLevel()) {
                npcChat(npc.id, DialogueEmote.DEFAULT, "I see that you have trained up all your skills.", "I am utmost impressed!")
                npcChat(npc.id, DialogueEmote.DEFAULT, "Would you like to purchase this cape on my back?", "It will cost you 13.37 million coins.")
                options(
                    title = "Purchase the max cape?",
                    DialogueOption("Yes") {
                        action { c ->
                            val coins = 13_370_000
                            val freeSlots = if (c.getInvAmt(995) == coins) 1 else 2
                            when {
                                c.freeSlots() < freeSlots -> c.send(
                                    SendMessage("You need at least ${if (freeSlots == 1) "one" else "two"} free inventory slot${if (freeSlots != 1) "s" else ""}.")
                                )
                                !c.playerHasItem(995, coins) -> c.send(
                                    SendMessage("You are missing ${NumberFormat.getNumberInstance().format(coins - c.getInvAmt(995))} coins!")
                                )
                                else -> {
                                    c.deleteItem(995, coins)
                                    c.addItem(13281, 1)
                                    c.addItem(13280, 1)
                                    c.checkItemUpdate()
                                    c.send(SendMessage("Here you go. Max cape just for you."))
                                }
                            }
                        }
                        finish()
                    },
                    DialogueOption("No") {
                        playerChat(DialogueEmote.DEFAULT, "No thanks.")
                        finish()
                    },
                )
            } else {
                npcChat(npc.id, DialogueEmote.DEFAULT, "You are quite weak!")
                finish()
            }
        }
        return true
    }
}
