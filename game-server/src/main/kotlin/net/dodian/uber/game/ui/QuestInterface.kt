@file:Suppress("unused")

package net.dodian.uber.game.ui

import net.dodian.uber.game.model.player.quests.QuestSend

object QuestInterface : InterfaceButtonContent {
    private val menuButtons: IntArray =
        (
            QuestSend.values()
                .map { it.clickId }
                .filter { it != -1 } +
                listOf(7333, 7383, 7339, 7334, 7338, 7340, 7341)
            ).distinct().toIntArray()

    override val bindings =
        listOf(
            buttonBinding(
                interfaceId = -1,
                componentId = 0,
                componentKey = "quests.menu",
                rawButtonIds = menuButtons,
            ) { client, request ->
                QuestSend.questMenu(client, request.rawButtonId)
            },
        )
}
