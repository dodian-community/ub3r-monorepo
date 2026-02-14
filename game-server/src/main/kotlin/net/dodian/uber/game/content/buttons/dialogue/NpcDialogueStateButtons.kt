package net.dodian.uber.game.content.buttons.dialogue

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.content.dialogue.legacy.core.DialogueIds
import net.dodian.uber.game.model.entity.player.Client

object NpcDialogueStateButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(150, 151)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            150 -> {
                client.NpcDialogue = DialogueIds.Legacy.TOGGLE_SPECIALS
                client.NpcDialogueSend = false
            }

            151 -> {
                client.NpcDialogue = DialogueIds.Legacy.TOGGLE_BOSS_YELL
                client.NpcDialogueSend = false
            }

            else -> return false
        }
        return true
    }
}
