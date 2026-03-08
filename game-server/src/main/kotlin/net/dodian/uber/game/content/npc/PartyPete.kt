package net.dodian.uber.game.content.npc

import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.dialogue.core.DialogueIds
import net.dodian.uber.game.content.dialogue.core.DialogueRenderRegistry
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.Frame171
import net.dodian.uber.game.netty.listener.out.NpcDialogueHead
import net.dodian.uber.game.netty.listener.out.SendString

internal object PartyPete {
    val npcIds: IntArray = intArrayOf(659)

    @Suppress("UNUSED_PARAMETER")
    fun onFirstClick(client: Client, npc: Npc): Boolean {
        client.startNpcDialogue(1000, npc.id)
        client.convoId = 1001
        return true
    }

    fun registerDialogueRenders(builder: DialogueRenderRegistry.Builder) {
        builder.handle(DialogueIds.Classic.GAMBLER_GREETING) { c ->
            c.sendFrame200(4883, c.npcFace)
            c.send(SendString(c.GetNpcName(DialogueService.activeNpcId(c)).replace("_", " "), 4884))
            c.send(SendString("Hi there, what would you like to do?", 4885))
            c.send(SendString("Click here to continue", 4886))
            c.send(NpcDialogueHead(DialogueService.activeNpcId(c), 4883))
            c.sendFrame164(4882)
            DialogueService.setDialogueSent(c, true)
            DialogueService.setNextDialogueId(c, DialogueIds.Classic.GAMBLER_OPTIONS)
            true
        }

        builder.handle(DialogueIds.Classic.GAMBLER_OPTIONS) { c ->
            c.send(Frame171(1, 2465))
            c.send(Frame171(0, 2468))
            c.send(SendString("What would you like to do?", 2460))
            c.send(SendString("Gamble", 2461))
            c.send(SendString("Nothing", 2462))
            c.sendFrame164(2459)
            DialogueService.setDialogueSent(c, true)
            true
        }

        builder.handle(DialogueIds.Classic.GAMBLER_BUSY) { c ->
            c.sendFrame200(4883, c.npcFace)
            c.send(SendString(c.GetNpcName(DialogueService.activeNpcId(c)).replace("_", " "), 4884))
            c.send(SendString("Cant talk right now!", 4885))
            c.send(SendString("Click here to continue", 4886))
            c.send(NpcDialogueHead(DialogueService.activeNpcId(c), 4883))
            c.sendFrame164(4882)
            DialogueService.setDialogueSent(c, true)
            true
        }
    }
}
