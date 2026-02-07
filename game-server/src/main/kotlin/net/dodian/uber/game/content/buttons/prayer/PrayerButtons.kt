package net.dodian.uber.game.content.buttons.prayer

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.prayer.Prayers

object PrayerButtons : ButtonContent {
    override val buttonIds: IntArray = Prayers.Prayer.values().map { it.buttonId }.toIntArray()

    override fun onClick(client: Client, buttonId: Int): Boolean {
        val prayer = Prayers.Prayer.forButton(buttonId) ?: return false
        client.prayerManager.togglePrayer(prayer)
        return true
    }
}
