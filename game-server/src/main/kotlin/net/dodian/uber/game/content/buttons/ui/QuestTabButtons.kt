package net.dodian.uber.game.content.buttons.ui

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

object QuestTabButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(83097)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.questPage = if (client.questPage == 0) 1 else 0
        return true
    }
}
