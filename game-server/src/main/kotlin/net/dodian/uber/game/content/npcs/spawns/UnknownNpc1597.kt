package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.content.dialogue.legacy.core.DialogueIds
import net.dodian.uber.game.content.dialogue.legacy.core.DialogueRegistry
import net.dodian.uber.game.netty.listener.out.SendString

internal object UnknownNpc1597 {
    val npcIds: IntArray = intArrayOf(1597)

    fun registerLegacyDialogues(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Legacy.MAGE_ARENA_OPTIONS) { c ->
            c.sendFrame200(4883, 1597)
            c.send(SendString(c.GetNpcName(c.NpcTalkTo), 4884))
            c.send(SendString("Select an Option", 2460))
            c.send(SendString("Can you teleport me to the mage arena?", 2461))
            c.send(SendString("Whats at the mage arena?", 2462))
            c.sendFrame164(2459)
            c.NpcDialogueSend = true
            true
        }
    }
}
