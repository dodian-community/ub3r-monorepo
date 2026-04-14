package net.dodian.uber.game.skill.thieving.dialogue

import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.engine.systems.dialogue.DialogueIds
import net.dodian.uber.game.engine.systems.dialogue.core.DialogueRenderRegistry
import net.dodian.uber.game.engine.systems.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.ui.dialogue.DialogueUi

object PyramidPlunderDialogueModule : DialogueRenderModule {
    override fun register(builder: DialogueRenderRegistry.Builder) {
        builder.handle(DialogueIds.Misc.PYRAMID_EXIT) { c ->
            DialogueUi.showPlayerOption(c, arrayOf("Exit pyramid plunder?", "Yes", "No"))
            DialogueService.setDialogueSent(c, true)
            true
        }
    }
}
