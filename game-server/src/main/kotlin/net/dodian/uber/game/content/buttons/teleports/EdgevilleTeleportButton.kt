package net.dodian.uber.game.content.buttons.teleports

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Misc

object EdgevilleTeleportButton : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(13095)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.triggerTele(3085 + Misc.random(4), 3488 + Misc.random(4), 0, false)
        return true
    }
}

