package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendFrame27

internal object BabaYaga {
    // Stats: 3837: r=0 a=0 d=0 s=0 hp=0 rg=0 mg=0

    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 3837, x = 2461, y = 3428, z = 0, face = 0),
    )

    val npcIds: IntArray = entries.map { it.npcId }.distinct().toIntArray()

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        val staffCounting = if (client.dailyReward.isEmpty()) 0 else Integer.parseInt(client.dailyReward[2])
        DialogueService.start(client) {
            if (staffCounting > 0) {
                npcChat(
                    npc.id,
                    DialogueEmote.EVIL3,
                    "Fancy meeting you here mysterious one.",
                    "I have $staffCounting battlestaffs that",
                    "you can claim for 7000 coins each.",
                )
                options(
                    title = "Do you wish to claim your battlestaffs?",
                    DialogueOption("Yes") {
                        action { c ->
                            c.send(RemoveInterfaces())
                            c.XinterfaceID = 3838
                            c.send(SendFrame27())
                        }
                        finish(closeInterfaces = false)
                    },
                    DialogueOption("No") {
                        finish()
                    },
                )
            } else {
                npcChat(
                    npc.id,
                    DialogueEmote.DISTRESSED2,
                    "Fancy meeting you here mysterious one.",
                    "I have no battlestaffs that you can claim.",
                )
                finish()
            }
        }
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSecondClick(client: Client, npc: Npc): Boolean {
        client.WanneShop = 33
        return true
    }
}
