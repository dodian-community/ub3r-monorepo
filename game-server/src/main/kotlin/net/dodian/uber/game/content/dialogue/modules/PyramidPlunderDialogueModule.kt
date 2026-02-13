package net.dodian.uber.game.content.dialogue.modules

import net.dodian.uber.game.content.dialogue.legacy.core.DialogueIds
import net.dodian.uber.game.content.dialogue.legacy.core.DialogueRegistry
import net.dodian.uber.game.content.dialogue.legacy.core.DialogueRenderModule
import net.dodian.uber.game.content.dialogue.legacy.core.DialogueUi

object PyramidPlunderDialogueModule : DialogueRenderModule {
    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Misc.PYRAMID_EXIT) { c ->
            DialogueUi.showPlayerOption(c, arrayOf("Exit pyramid plunder?", "Yes", "No"))
            c.NpcDialogueSend = true
            true
        }
    }
}
