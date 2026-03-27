package net.dodian.uber.game.systems.ui.interfaces.quests

import net.dodian.uber.game.model.player.quests.QuestSend
import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding

object QuestInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        listOf(
            buttonBinding(
                interfaceId = -1,
                componentId = 0,
                componentKey = "quests.menu",
                rawButtonIds = QuestComponents.menuButtons,
            ) { client, request ->
                QuestSend.questMenu(client, request.rawButtonId)
            }
        )
}
