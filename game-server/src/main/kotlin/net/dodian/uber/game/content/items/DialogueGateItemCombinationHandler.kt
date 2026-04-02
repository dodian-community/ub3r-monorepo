package net.dodian.uber.game.content.items

import net.dodian.uber.game.systems.ui.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client

object DialogueGateItemCombinationHandler {
    private const val ROCKSHELL_DIALOGUE_ID = 10000

    @JvmStatic
    fun handle(client: Client, itemUsed: Int, useWith: Int): Boolean {
        if (itemUsed !in 6157..6161 || useWith !in 6157..6161) {
            return false
        }
        DialogueService.setDialogueSent(client, false)
        DialogueService.setDialogueId(client, ROCKSHELL_DIALOGUE_ID)
        return true
    }
}
