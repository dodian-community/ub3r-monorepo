package net.dodian.uber.game.objects.travel.dialogue

import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.engine.systems.dialogue.DialogueIds
import net.dodian.uber.game.engine.systems.dialogue.core.DialogueRenderRegistry
import net.dodian.uber.game.engine.systems.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.ui.dialogue.DialogueUi

object BrimhavenEntryDialogueModule : DialogueRenderModule {
    override fun register(builder: DialogueRenderRegistry.Builder) {
        builder.handle(DialogueIds.Misc.BRIMHAVEN_ENTRY) { c ->
            DialogueUi.showPlayerOption(c, arrayOf("Do you wish to enter?", "Sacrifice 5 dragon bones", "Stay here"))
            DialogueService.setDialogueSent(c, true)
            true
        }
    }
}

