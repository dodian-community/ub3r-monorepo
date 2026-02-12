package net.dodian.uber.game.content.npcs.dialogue

import net.dodian.uber.game.content.dialogue.text.DialoguePagingService
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueUi
import net.dodian.uber.game.model.entity.player.Client

object NpcDialogueService {

    @JvmStatic
    fun updateNpcChat(client: Client) {
        DialogueRegistry.render(client)
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
