package net.dodian.uber.game.ui

import net.dodian.uber.game.skill.prayer.PrayerManager
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

object PrayerInterface : InterfaceButtonContent {
    private const val INTERFACE_ID = 5608

    private data class PrayerBinding(
        val componentId: Int,
        val componentKey: String,
        val prayer: PrayerManager.Prayer,
        val rawButtonIds: IntArray,
    )

    private val prayers: List<PrayerBinding> =
        PrayerManager.Prayer.values().mapIndexed { index, prayer ->
            PrayerBinding(
                componentId = index,
                componentKey = "prayer.${prayer.name.lowercase()}",
                prayer = prayer,
                rawButtonIds = intArrayOf(prayer.buttonId),
            )
        }

    override val bindings =
        prayers.map { binding ->
            buttonBinding(
                interfaceId = INTERFACE_ID,
                componentId = binding.componentId,
                componentKey = binding.componentKey,
                rawButtonIds = binding.rawButtonIds,
            ) { client, _ ->
                client.prayerManager.togglePrayer(binding.prayer)
                true
            }
        }
}
