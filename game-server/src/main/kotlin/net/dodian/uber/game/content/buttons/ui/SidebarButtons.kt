package net.dodian.uber.game.content.buttons.ui

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

object SidebarButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(7212)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.setSidebarInterface(0, 328)
        return true
    }
}

