package net.dodian.uber.game.content.buttons.teleports

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Misc

object YanilleHomeTeleportButton : ButtonContent {
    override val buttonIds: IntArray = SpellbookTeleportButtonIds.YANILLE

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.triggerTele(2604 + Misc.random(6), 3101 + Misc.random(3), 0, false)
        return true
    }
}
