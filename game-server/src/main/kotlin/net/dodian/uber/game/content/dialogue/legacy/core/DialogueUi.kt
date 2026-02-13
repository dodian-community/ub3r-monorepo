package net.dodian.uber.game.content.dialogue.legacy.core

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.Frame171
import net.dodian.uber.game.netty.listener.out.NpcDialogueHead
import net.dodian.uber.game.netty.listener.out.PlayerDialogueHead
import net.dodian.uber.game.netty.listener.out.SendString

object DialogueUi {

    @JvmStatic
    fun showPlayerOption(client: Client, text: Array<String>) = with(client) {
        var base = 2459
        if (text.size == 4) base = 2469
        if (text.size == 5) base = 2480
        if (text.size == 6) base = 2492

        send(Frame171(1, base + 4 + text.size - 1))
        send(Frame171(0, base + 7 + text.size - 1))
        for (i in text.indices) {
            send(SendString(text[i], base + 1 + i))
        }
        sendFrame164(base)
    }

    @JvmStatic
    fun showNpcChat(client: Client, npcId: Int, emote: Int, text: Array<String>) = with(client) {
        var base = 4882
        if (text.size == 2) base = 4887
        if (text.size == 3) base = 4893
        if (text.size == 4) base = 4900

        sendFrame200(base + 1, emote)
        send(SendString(GetNpcName(npcId), base + 2))
        for (i in text.indices) {
            send(SendString(text[i], base + 3 + i))
        }
        send(SendString("Click here to continue", base + 3 + text.size))
        send(NpcDialogueHead(npcId, base + 1))
        sendFrame164(base)
    }

    @JvmStatic
    fun showPlayerChat(client: Client, text: Array<String>, emote: Int) = with(client) {
        var base = 968
        if (text.size == 2) base = 973
        if (text.size == 3) base = 979
        if (text.size == 4) base = 986

        send(PlayerDialogueHead(base + 1))
        sendFrame200(base + 1, emote)
        send(SendString(playerName, base + 2))
        for (i in text.indices) {
            send(SendString(text[i], base + 3 + i))
        }
        sendFrame164(base)
    }
}
