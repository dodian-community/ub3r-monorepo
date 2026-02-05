package net.dodian.uber.game.content.buttons.settings

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage

object BossYellButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        24136, // enable
        24137, // disable
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            24136 -> {
                client.yellOn = true
                client.send(SendMessage("You enabled the boss yell messages."))
            }
            24137 -> {
                client.yellOn = false
                client.send(SendMessage("You disabled the boss yell messages."))
            }
            else -> return false
        }
        return true
    }
}

