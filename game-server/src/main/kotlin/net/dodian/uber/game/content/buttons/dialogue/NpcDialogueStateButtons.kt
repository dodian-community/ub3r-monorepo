package net.dodian.uber.game.content.buttons.dialogue

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.dialogue.core.DialogueIds
import net.dodian.uber.game.model.entity.player.Client

object NpcDialogueStateButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(150, 151)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            150 -> {
                DialogueService.setDialogueId(client, DialogueIds.Classic.TOGGLE_SPECIALS)
                DialogueService.setDialogueSent(client, false)
            }

            151 -> {
                DialogueService.setDialogueId(client, DialogueIds.Classic.TOGGLE_BOSS_YELL)
                DialogueService.setDialogueSent(client, false)
            }

            else -> return false
        }
        return true
    }
}
