package net.dodian.uber.game.content.buttons.quests

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.quests.QuestSend

object QuestMenuButtons : ButtonContent {
    override val buttonIds: IntArray = (
        QuestSend.values()
            .map { it.clickId }
            .filter { it != -1 } +
            listOf(7333, 7383, 7339, 7334, 7338, 7340, 7341)
        ).distinct().toIntArray()

    override fun onClick(client: Client, buttonId: Int): Boolean {
        return QuestSend.questMenu(client, buttonId)
    }
}
