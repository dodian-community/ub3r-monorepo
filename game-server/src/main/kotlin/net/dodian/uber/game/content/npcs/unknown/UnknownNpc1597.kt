package net.dodian.uber.game.content.npcs.unknown

import net.dodian.uber.game.content.npcs.*

import net.dodian.uber.game.systems.ui.dialogue.core.DialogueIds
import net.dodian.uber.game.systems.ui.dialogue.core.DialogueRegistry
import net.dodian.uber.game.netty.listener.out.SendString

internal object UnknownNpc1597 {
    val npcIds: IntArray = intArrayOf(1597)

    fun registerLegacyDialogues(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Legacy.MAGE_ARENA_OPTIONS) { c ->
            c.sendInterfaceAnimation(4883, 1597)
            c.sendString(c.GetNpcName(c.NpcTalkTo), 4884)
            c.sendString("Select an Option", 2460)
            c.sendString("Can you teleport me to the mage arena?", 2461)
            c.sendString("Whats at the mage arena?", 2462)
            c.sendChatboxInterface(2459)
            c.NpcDialogueSend = true
            true
        }
    }
}
