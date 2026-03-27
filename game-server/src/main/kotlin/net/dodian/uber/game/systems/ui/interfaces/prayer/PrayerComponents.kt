package net.dodian.uber.game.systems.ui.interfaces.prayer

import net.dodian.uber.game.model.player.skills.prayer.Prayers

object PrayerComponents {
    const val INTERFACE_ID = 5608

    data class PrayerBinding(
        val componentId: Int,
        val componentKey: String,
        val prayer: Prayers.Prayer,
        val rawButtonIds: IntArray,
    )

    val prayers: List<PrayerBinding> =
        Prayers.Prayer.values().mapIndexed { index, prayer ->
            PrayerBinding(
                componentId = index,
                componentKey = "prayer.${prayer.name.lowercase()}",
                prayer = prayer,
                rawButtonIds = intArrayOf(prayer.buttonId),
            )
        }
}

