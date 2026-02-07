package net.dodian.uber.game.content.buttons.quests

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.quests.QuestSend

object QuestMenuButtons : ButtonContent {
    override val buttonIds: IntArray = (
        QuestSend.values()
            .map { it.clickId }
            .filter { it != -1 } +
            listOf(28165, 28215, 28171, 28166, 28170, 28172, 28173)
        ).distinct().toIntArray()

    override fun onClick(client: Client, buttonId: Int): Boolean {
        return QuestSend.questMenu(client, buttonId)
    }
}
