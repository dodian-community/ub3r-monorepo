package net.dodian.uber.game.content.buttons.magic.teleports

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Misc

object AncientTeleportButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        *SpellbookTeleportButtonIds.YANILLE,
        *SpellbookTeleportButtonIds.SEERS,
        *SpellbookTeleportButtonIds.ARDOUGNE,
        *SpellbookTeleportButtonIds.CATHERBY,
        *SpellbookTeleportButtonIds.LEGENDS_GUILD,
        *SpellbookTeleportButtonIds.TAVERLY,
        *SpellbookTeleportButtonIds.FISHING_GUILD,
        *SpellbookTeleportButtonIds.GNOME_VILLAGE,
        *SpellbookTeleportButtonIds.EDGEVILLE,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            in SpellbookTeleportButtonIds.YANILLE -> client.triggerTele(2604 + Misc.random(6), 3101 + Misc.random(3), 0, false)
            in SpellbookTeleportButtonIds.SEERS -> client.triggerTele(2722 + Misc.random(6), 3484 + Misc.random(2), 0, false)
            in SpellbookTeleportButtonIds.ARDOUGNE -> client.triggerTele(2660 + Misc.random(4), 3306 + Misc.random(4), 0, false)
            in SpellbookTeleportButtonIds.CATHERBY -> client.triggerTele(2802 + Misc.random(4), 3432 + Misc.random(3), 0, false)
            in SpellbookTeleportButtonIds.LEGENDS_GUILD -> client.triggerTele(2726 + Misc.random(5), 3346 + Misc.random(2), 0, false)
            in SpellbookTeleportButtonIds.TAVERLY -> client.triggerTele(2893 + Misc.random(4), 3454 + Misc.random(3), 0, false)
            in SpellbookTeleportButtonIds.FISHING_GUILD -> client.triggerTele(2596 + Misc.random(3), 3406 + Misc.random(4), 0, true)
            in SpellbookTeleportButtonIds.GNOME_VILLAGE -> client.triggerTele(2472 + Misc.random(6), 3436 + Misc.random(3), 0, false)
            in SpellbookTeleportButtonIds.EDGEVILLE -> client.triggerTele(3085 + Misc.random(4), 3488 + Misc.random(4), 0, false)
            else -> return false
        }
        return true
    }
}
