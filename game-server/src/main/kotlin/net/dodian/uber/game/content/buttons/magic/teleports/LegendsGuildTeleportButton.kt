package net.dodian.uber.game.content.buttons.magic.teleports

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Misc

object LegendsGuildTeleportButton : ButtonContent {
    override val buttonIds: IntArray = SpellbookTeleportButtonIds.LEGENDS_GUILD

    override fun onClick(client: Client, buttonId: Int): Boolean {
        client.triggerTele(2726 + Misc.random(5), 3346 + Misc.random(2), 0, false)
        return true
    }
}
