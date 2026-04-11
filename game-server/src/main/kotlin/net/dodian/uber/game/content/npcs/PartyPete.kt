package net.dodian.uber.game.content.npcs

import net.dodian.uber.game.content.social.dialogue.core.DialogueIds
import net.dodian.uber.game.content.social.dialogue.core.DialogueRegistry
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.Frame171
import net.dodian.uber.game.netty.listener.out.NpcDialogueHead
import net.dodian.uber.game.netty.listener.out.SendString

internal object PartyPete {
    val npcIds: IntArray = intArrayOf(659)

    @Suppress("UNUSED_PARAMETER")
    fun onFirstClick(client: Client, npc: Npc): Boolean {
        client.NpcWanneTalk = 1000
        client.convoId = 1001
        return true
    }

    fun registerLegacyDialogues(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Legacy.GAMBLER_GREETING) { c ->
            c.sendInterfaceAnimation(4883, c.npcFace)
            c.sendString(c.GetNpcName(c.NpcTalkTo).replace("_", " "), 4884)
            c.sendString("Hi there, what would you like to do?", 4885)
            c.sendString("Click here to continue", 4886)
            c.send(NpcDialogueHead(c.NpcTalkTo, 4883))
            c.sendChatboxInterface(4882)
            c.NpcDialogueSend = true
            c.nextDiag = DialogueIds.Legacy.GAMBLER_OPTIONS
            true
        }

        builder.handle(DialogueIds.Legacy.GAMBLER_OPTIONS) { c ->
            c.send(Frame171(1, 2465))
            c.send(Frame171(0, 2468))
            c.sendString("What would you like to do?", 2460)
            c.sendString("Gamble", 2461)
            c.sendString("Nothing", 2462)
            c.sendChatboxInterface(2459)
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Legacy.GAMBLER_BUSY) { c ->
            c.sendInterfaceAnimation(4883, c.npcFace)
            c.sendString(c.GetNpcName(c.NpcTalkTo).replace("_", " "), 4884)
            c.sendString("Cant talk right now!", 4885)
            c.sendString("Click here to continue", 4886)
            c.send(NpcDialogueHead(c.NpcTalkTo, 4883))
            c.sendChatboxInterface(4882)
            c.NpcDialogueSend = true
            true
        }
    }
}
