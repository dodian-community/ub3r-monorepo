package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.Frame171
import net.dodian.uber.game.netty.listener.out.NpcDialogueHead
import net.dodian.uber.game.netty.listener.out.PlayerDialogueHead
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.party.Balloons
import net.dodian.utilities.Utils

internal object Aubury {
    // Stats: 637: r=0 a=0 d=0 s=0 hp=0 rg=0 mg=0

    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 637, x = 2594, y = 3104, z = 0, face = 0),
        NpcSpawnDef(npcId = 637, x = 3253, y = 3402, z = 0, face = 0),
    )

    val npcIds: IntArray = entries.map { it.npcId }.distinct().toIntArray()

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

object RuneShopDialogueModule : DialogueRenderModule {

    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Legacy.RUNE_SHOP_GREETING) { c ->
            c.sendFrame200(4883, 591)
            c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4884))
            c.send(SendString("Do you want to buy some magical gear?", 4885))
            c.send(SendString("Click here to continue", 4886))
            c.send(NpcDialogueHead(c.NpcTalkTo, 4883))
            c.sendFrame164(4882)
            c.NpcDialogueSend = true
            c.nextDiag = DialogueIds.Legacy.RUNE_SHOP_OPTIONS
            true
        }

        builder.handle(DialogueIds.Legacy.RUNE_SHOP_OPTIONS) { c ->
            c.send(Frame171(1, 2465))
            c.send(Frame171(0, 2468))
            c.send(SendString("Select an Option", 2460))
            c.send(SendString("Yes please!", 2461))
            c.send(SendString("Oh it's a rune shop. No thank you, then.", 2462))
            c.sendFrame164(2459)
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Legacy.PLAYER_NO_RUNE_SHOP) { c ->
            c.sendFrame200(969, 974)
            c.send(SendString(c.playerName, 970))
            c.send(SendString("Oh it's a rune shop. No thank you, then.", 971))
            c.send(SendString("Click here to continue", 972))
            c.send(PlayerDialogueHead(969))
            c.sendFrame164(968)
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Legacy.RUNE_SHOP_REPLY) { c ->
            c.sendFrame200(4888, 592)
            c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4889))
            c.send(SendString("Well, if you find somone who does want runes, please", 4890))
            c.send(SendString("send them my way.", 4891))
            c.send(SendString("Click here to continue", 4892))
            c.send(NpcDialogueHead(c.NpcTalkTo, 4888))
            c.sendFrame164(4887)
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Legacy.RUNE_SHOP_REPLY_ONE_LINE) { c ->
            c.sendFrame200(4883, 591)
            c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4884))
            c.send(SendString("Well, if you find somone who does want runes, please send them my way.", 4885))
            c.send(SendString("Click here to continue", 4886))
            c.send(NpcDialogueHead(c.NpcTalkTo, 4883))
            c.sendFrame164(4882)
            c.NpcDialogueSend = true
            true
        }
    }
}
