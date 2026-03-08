package net.dodian.uber.game.content.dialogue

import net.dodian.uber.game.model.entity.player.Client

object LegacyDialogueTickBridge {
    @JvmStatic
    fun flushIfNeeded(client: Client) {
        if (DialogueService.hasActiveSession(client)) {
            return
        }
        if (client.NpcDialogue > 0 && !client.NpcDialogueSend) {
            DialogueDisplayService.updateNpcChat(client)
        }
    }
}
