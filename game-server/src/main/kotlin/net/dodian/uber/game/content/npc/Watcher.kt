package net.dodian.uber.game.content.npc

import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.dialogue.core.DialogueIds
import net.dodian.uber.game.content.dialogue.core.DialogueRenderRegistry
import net.dodian.uber.game.netty.listener.out.NpcDialogueHead
import net.dodian.uber.game.netty.listener.out.SendString

internal object Watcher {
    val npcIds: IntArray = intArrayOf(804)

    fun registerDialogueRenders(builder: DialogueRenderRegistry.Builder) {
        builder.handle(DialogueIds.Classic.WATCHER_MESSAGE) { c ->
            c.sendFrame200(4883, 804)
            c.send(SendString(c.GetNpcName(804), 4884))
            c.send(SendString(c.dMsg, 4885))
            c.send(NpcDialogueHead(804, 4883))
            c.sendFrame164(4882)
            DialogueService.setDialogueSent(c, true)
            true
        }
    }
}
