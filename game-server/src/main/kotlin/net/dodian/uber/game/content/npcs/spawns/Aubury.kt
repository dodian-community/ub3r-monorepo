package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.party.Balloons
import net.dodian.utilities.Utils

internal object Aubury {
    // Stats: 637: r=0 a=0 d=0 s=0 hp=0 rg=0 mg=0

    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 637, x = 2594, y = 3104, z = 0, face = 0),
        NpcSpawnDef(npcId = 637, x = 3253, y = 3402, z = 0, face = 0),
    )

    val npcIds: IntArray = npcIdsFromEntries(entries)

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        DialogueService.start(client) {
            npcChat(npc.id, DialogueEmote.DEFAULT, "Do you want to buy some magical gear?")
            options(
                title = "Select an Option",
                DialogueOption("Yes please!") {
                    action { c -> c.openUpShop(9) }
                    finish(closeInterfaces = false)
                },
                DialogueOption("Oh it's a rune shop. No thank you, then.") {
                    playerChat(DialogueEmote.DEFAULT, "Oh it's a rune shop. No thank you, then.")
                    npcChat(
                        npc.id,
                        DialogueEmote.EVIL1,
                        "Well, if you find somone who does want runes, please",
                        "send them my way.",
                    )
                    finish()
                },
            )
        }
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSecondClick(client: Client, npc: Npc): Boolean {
        client.WanneShop = 9
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    fun onThirdClick(client: Client, npc: Npc): Boolean {
        if (Balloons.eventActive()) {
            client.triggerTele(3045, 3372, 0, false)
            client.send(SendMessage("Welcome to the party room!"))
        } else {
            client.triggerTele(3086 + Utils.random(2), 3488 + Utils.random(2), 0, false)
            client.send(SendMessage("Welcome to Edgeville!"))
        }
        return true
    }
}
