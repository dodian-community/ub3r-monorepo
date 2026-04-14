package net.dodian.uber.game.skill.smithing.rockshell

import net.dodian.uber.game.engine.systems.dialogue.DialogueIds
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client

object DialogueGateItemCombinations {
    @JvmStatic
    fun handle(client: Client, itemUsed: Int, useWith: Int): Boolean {
        if (itemUsed !in 6157..6161 || useWith !in 6157..6161) {
            return false
        }
        DialogueService.setDialogueSent(client, false)
        DialogueService.setDialogueId(client, DialogueIds.Misc.ROCKSHELL_MENU)
        return true
    }
}

