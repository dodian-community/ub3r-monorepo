package net.dodian.uber.game.content.npcs.dialogue.modules

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.netty.listener.out.Frame171
import net.dodian.uber.game.netty.listener.out.NpcDialogueHead
import net.dodian.uber.game.netty.listener.out.PlayerDialogueHead
import net.dodian.uber.game.netty.listener.out.SendString

/**
 * Handles rune-shop greeting and branch responses.
 * - Dialogue IDs: 3, 4, 5, 6, 7
 * - NPC IDs: 637
 */
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
