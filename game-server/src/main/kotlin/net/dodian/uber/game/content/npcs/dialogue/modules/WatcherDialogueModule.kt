package net.dodian.uber.game.content.npcs.dialogue.modules

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.netty.listener.out.NpcDialogueHead
import net.dodian.uber.game.netty.listener.out.SendString

/**
 * Handles dynamic watcher/admin message prompt.
 * - Dialogue ID: 10
 * - NPC ID: 804
 */
object WatcherDialogueModule : DialogueRenderModule {

    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Legacy.WATCHER_MESSAGE) { c ->
            c.sendFrame200(4883, 804)
            c.send(SendString(c.GetNpcName(804), 4884))
            c.send(SendString(c.dMsg, 4885))
            c.send(NpcDialogueHead(804, 4883))
            c.sendFrame164(4882)
            c.NpcDialogueSend = true
            true
        }
    }
}
