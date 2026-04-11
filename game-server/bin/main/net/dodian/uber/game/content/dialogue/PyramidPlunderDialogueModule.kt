package net.dodian.uber.game.content.dialogue

import net.dodian.uber.game.content.social.dialogue.DialogueService
import net.dodian.uber.game.content.social.dialogue.core.DialogueIds
import net.dodian.uber.game.content.social.dialogue.core.DialogueRenderRegistry
import net.dodian.uber.game.content.social.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.social.dialogue.core.DialogueUi

object PyramidPlunderDialogueModule : DialogueRenderModule {
    override fun register(builder: DialogueRenderRegistry.Builder) {
        builder.handle(DialogueIds.Misc.PYRAMID_EXIT) { c ->
            DialogueUi.showPlayerOption(c, arrayOf("Exit pyramid plunder?", "Yes", "No"))
            DialogueService.setDialogueSent(c, true)
            true
        }
    }
}
