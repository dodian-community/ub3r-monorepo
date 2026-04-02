package net.dodian.uber.game.content.npcs

import net.dodian.uber.game.systems.ui.dialogue.core.DialogueIds
import net.dodian.uber.game.systems.ui.dialogue.core.DialogueRegistry
import net.dodian.uber.game.netty.listener.out.NpcDialogueHead
import net.dodian.uber.game.netty.listener.out.SendString

internal object Watcher {
    val npcIds: IntArray = intArrayOf(804)

    fun registerLegacyDialogues(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Legacy.WATCHER_MESSAGE) { c ->
            c.sendInterfaceAnimation(4883, 804)
            c.sendString(c.GetNpcName(804), 4884)
            c.sendString(c.dMsg, 4885)
            c.send(NpcDialogueHead(804, 4883))
            c.sendChatboxInterface(4882)
            c.NpcDialogueSend = true
            true
        }
    }
}
