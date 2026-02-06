package net.dodian.uber.game.content.buttons.magic.teleports

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Misc

object TaverlyTeleportButton : ButtonContent {
    override val buttonIds: IntArray = SpellbookTeleportButtonIds.TAVERLY

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.triggerTele(2893 + Misc.random(4), 3454 + Misc.random(3), 0, false)
        return true
    }
}
