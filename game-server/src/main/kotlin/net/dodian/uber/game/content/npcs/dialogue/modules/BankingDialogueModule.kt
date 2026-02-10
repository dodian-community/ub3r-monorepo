package net.dodian.uber.game.content.npcs.dialogue.modules

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.netty.listener.out.Frame171
import net.dodian.uber.game.netty.listener.out.NpcDialogueHead
import net.dodian.uber.game.netty.listener.out.SendString

/**
 * Handles banking conversation and PIN prompt.
 * - Dialogue IDs: 1, 2, 8
 * - NPC IDs: 394, 395, 7677
 */
object BankingDialogueModule : DialogueRenderModule {

    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Legacy.BANK_GREETING) { c ->
            c.sendFrame200(4883, 591)
            c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4884))
            c.send(SendString("Good day, how can I help you?", 4885))
            c.send(SendString("Click here to continue", 4886))
            c.send(NpcDialogueHead(c.NpcTalkTo, 4883))
            c.sendFrame164(4882)
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Legacy.BANK_OPTIONS) { c ->
            c.send(Frame171(1, 2465))
            c.send(Frame171(0, 2468))
            c.send(SendString("What would you like to say?", 2460))
            c.send(SendString("I'd like to access my bank account, please.", 2461))
            c.send(SendString("I'd like to check my PIN settings.", 2462))
            c.sendFrame164(2459)
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Legacy.PIN_NOT_IMPLEMENTED) { c ->
            c.sendFrame200(4883, 591)
            c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4884))
            c.send(SendString("Pins have not been implemented yet", 4885))
            c.send(SendString("Click here to continue", 4886))
            c.send(NpcDialogueHead(c.NpcTalkTo, 4883))
            c.sendFrame164(4882)
            c.NpcDialogueSend = true
            true
        }
    }
}
