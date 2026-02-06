package net.dodian.uber.game.content.buttons.magic.teleports

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Misc

object GnomeVillageTeleportButton : ButtonContent {
    override val buttonIds: IntArray = SpellbookTeleportButtonIds.GNOME_VILLAGE

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.triggerTele(2472 + Misc.random(6), 3436 + Misc.random(3), 0, false)
        return true
    }
}
