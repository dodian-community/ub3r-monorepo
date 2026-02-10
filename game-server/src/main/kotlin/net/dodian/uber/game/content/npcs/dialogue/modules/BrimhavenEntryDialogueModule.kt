package net.dodian.uber.game.content.npcs.dialogue.modules

import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueUi

/**
 * Handles Brimhaven sacrifice prompt.
 * - Dialogue ID: 536
 */
object BrimhavenEntryDialogueModule : DialogueRenderModule {

    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Misc.BRIMHAVEN_ENTRY) { c ->
            DialogueUi.showPlayerOption(c, arrayOf("Do you wish to enter?", "Sacrifice 5 dragon bones", "Stay here"))
            c.NpcDialogueSend = true
            true
        }
    }
}
