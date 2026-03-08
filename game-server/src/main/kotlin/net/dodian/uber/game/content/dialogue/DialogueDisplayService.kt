package net.dodian.uber.game.content.dialogue

import net.dodian.uber.game.content.dialogue.text.DialoguePagingService
import net.dodian.uber.game.content.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.dialogue.core.DialogueUi
import net.dodian.uber.game.model.entity.player.Client

object DialogueDisplayService {

    @JvmStatic
    fun updateNpcChat(client: Client) {
        DialogueService.captureLegacyBridgeState(client)
        DialogueRegistry.render(client)
        DialogueService.captureLegacyBridgeState(client)
    }

    @JvmStatic
    fun showPlayerOption(client: Client, text: Array<String>) {
        DialogueUi.showPlayerOption(client, text)
    }

    @JvmStatic
    fun showNpcChat(client: Client, npcId: Int, emote: Int, text: Array<String>) {
        DialoguePagingService.showNpcChat(client, npcId, emote, text.joinToString("\n"))
    }

    @JvmStatic
    fun showPlayerChat(client: Client, text: Array<String>, emote: Int) {
        DialoguePagingService.showPlayerChat(client, emote, text.joinToString("\n"))
    }
}
