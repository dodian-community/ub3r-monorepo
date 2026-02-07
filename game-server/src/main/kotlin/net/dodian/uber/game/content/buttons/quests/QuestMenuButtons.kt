package net.dodian.uber.game.content.buttons.quests

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.quests.QuestSend

object QuestMenuButtons : ButtonContent {
    private val remappedButtonIds: Map<Int, Int> = mapOf(
        7332 to 28164, // Boss log
        7333 to 28165, // Monster log
        7334 to 28166, // Commands
        7383 to 28215, // News
        7339 to 28171, // Guides
        7338 to 28170, // Account services
        7340 to 28172, // Discord
        7341 to 28173, // Game CP
    )

    override val buttonIds: IntArray = (
        QuestSend.values()
            .map { it.clickId }
            .filter { it != -1 } +
            listOf(28165, 28215, 28171, 28166, 28170, 28172, 28173) +
            remappedButtonIds.keys
        ).distinct().toIntArray()

    override fun onClick(client: Client, buttonId: Int): Boolean {
        val mappedButtonId = remappedButtonIds[buttonId] ?: buttonId
        return QuestSend.questMenu(client, mappedButtonId)
    }
}
