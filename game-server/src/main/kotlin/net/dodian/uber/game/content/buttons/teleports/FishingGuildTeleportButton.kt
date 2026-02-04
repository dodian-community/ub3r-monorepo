package net.dodian.uber.game.content.buttons.teleports

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Misc

object FishingGuildTeleportButton : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(13079)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.triggerTele(2596 + Misc.random(3), 3406 + Misc.random(4), 0, true)
        return true
    }
}

