package net.dodian.uber.game.ui.dialogue

import net.dodian.uber.game.ui.dialogue.text.DialoguePagingService
import net.dodian.uber.game.engine.systems.dialogue.core.DialogueRenderRegistry
import net.dodian.uber.game.ui.dialogue.DialogueUi
import net.dodian.uber.game.model.entity.player.Client

object DialogueDisplayService {

    @JvmStatic
    fun updateNpcChat(client: Client) {
        DialogueRenderRegistry.render(client)
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
