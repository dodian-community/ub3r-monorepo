package net.dodian.uber.game.content.npc

import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.dialogue.core.DialogueIds
import net.dodian.uber.game.content.dialogue.core.DialogueRenderRegistry
import net.dodian.uber.game.netty.listener.out.SendString

internal object UnknownNpc1597 {
    val npcIds: IntArray = intArrayOf(1597)

    fun registerDialogueRenders(builder: DialogueRenderRegistry.Builder) {
        builder.handle(DialogueIds.Classic.MAGE_ARENA_OPTIONS) { c ->
            c.sendFrame200(4883, 1597)
            c.send(SendString(c.GetNpcName(DialogueService.activeNpcId(c)), 4884))
            c.send(SendString("Select an Option", 2460))
            c.send(SendString("Can you teleport me to the mage arena?", 2461))
            c.send(SendString("Whats at the mage arena?", 2462))
            c.sendFrame164(2459)
            DialogueService.setDialogueSent(c, true)
            true
        }
    }
}
