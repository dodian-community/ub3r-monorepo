@file:Suppress("unused")

package net.dodian.uber.game.ui

import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

object QuestInterface : InterfaceButtonContent {
    private val menuButtons: IntArray =
        (
            QuestTabEntry.values()
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
                QuestTabEntry.questMenu(client, request.rawButtonId)
            },
        )
}
