package net.dodian.uber.game.content.buttons

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object PinHelpButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(58073)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.send(SendMessage("Visit the Dodian.net UserCP and click edit pin to remove your pin"))
        return true
    }
}

