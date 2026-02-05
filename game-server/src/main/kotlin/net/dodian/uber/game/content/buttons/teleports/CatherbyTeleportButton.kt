package net.dodian.uber.game.content.buttons.teleports

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Misc

object CatherbyTeleportButton : ButtonContent {
    override val buttonIds: IntArray = SpellbookTeleportButtonIds.CATHERBY

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.triggerTele(2802 + Misc.random(4), 3432 + Misc.random(3), 0, false)
        return true
    }
}
