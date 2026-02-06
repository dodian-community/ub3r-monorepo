package net.dodian.uber.game.content.buttons.ui

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

object RunButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(152, 153, 74214)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            152 -> client.buttonOnRun = false
            153 -> client.buttonOnRun = true
            74214 -> client.buttonOnRun = !client.buttonOnRun
            else -> return false
        }
        return true
    }
}

