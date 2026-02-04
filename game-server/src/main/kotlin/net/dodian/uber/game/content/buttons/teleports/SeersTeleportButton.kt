package net.dodian.uber.game.content.buttons.teleports

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Misc

object SeersTeleportButton : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(13035)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.triggerTele(2722 + Misc.random(6), 3484 + Misc.random(2), 0, false)
        return true
    }
}

