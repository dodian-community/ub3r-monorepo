package net.dodian.uber.game.content.buttons.teleports

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Misc

object ArdougneTeleportButton : ButtonContent {
    override val buttonIds: IntArray = SpellbookTeleportButtonIds.ARDOUGNE

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.triggerTele(2660 + Misc.random(4), 3306 + Misc.random(4), 0, false)
        return true
    }
}
