package net.dodian.uber.game.content.npcs.dialogue.modules

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueUi

/**
 * Handles pyramid plunder exit confirm.
 * - Dialogue ID: 20931
 */
object PyramidPlunderDialogueModule : DialogueRenderModule {

    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Misc.PYRAMID_EXIT) { c ->
            DialogueUi.showPlayerOption(c, arrayOf("Exit pyramid plunder?", "Yes", "No"))
            c.NpcDialogueSend = true
            true
        }
    }
}
