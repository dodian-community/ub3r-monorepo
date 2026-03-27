package net.dodian.uber.game.systems.ui.interfaces.quests

import net.dodian.uber.game.model.player.quests.QuestSend

object QuestComponents {
    val menuButtons: IntArray =
        (
            QuestSend.values()
                .map { it.clickId }
                .filter { it != -1 } +
                listOf(7333, 7383, 7339, 7334, 7338, 7340, 7341)
            ).distinct().toIntArray()
}
