package net.dodian.uber.game.content.buttons.minigames

import net.dodian.uber.game.Server
import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

object SlotsButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(54074)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        Server.slots.playSlots(client, -1)
        return true
    }
}

