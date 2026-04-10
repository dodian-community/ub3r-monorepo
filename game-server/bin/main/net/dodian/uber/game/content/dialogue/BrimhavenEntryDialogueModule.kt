package net.dodian.uber.game.content.dialogue

import net.dodian.uber.game.systems.ui.dialogue.DialogueService
import net.dodian.uber.game.systems.ui.dialogue.core.DialogueIds
import net.dodian.uber.game.systems.ui.dialogue.core.DialogueRenderRegistry
import net.dodian.uber.game.systems.ui.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.systems.ui.dialogue.core.DialogueUi

object BrimhavenEntryDialogueModule : DialogueRenderModule {
    override fun register(builder: DialogueRenderRegistry.Builder) {
        builder.handle(DialogueIds.Misc.BRIMHAVEN_ENTRY) { c ->
            DialogueUi.showPlayerOption(c, arrayOf("Do you wish to enter?", "Sacrifice 5 dragon bones", "Stay here"))
            DialogueService.setDialogueSent(c, true)
            true
        }
    }
}
