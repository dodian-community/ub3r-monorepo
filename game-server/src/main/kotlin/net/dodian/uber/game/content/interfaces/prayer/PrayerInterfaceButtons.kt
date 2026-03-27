package net.dodian.uber.game.content.interfaces.prayer

import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding

object PrayerInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        PrayerComponents.prayers.map { binding ->
            buttonBinding(
                interfaceId = PrayerComponents.INTERFACE_ID,
                componentId = binding.componentId,
                componentKey = binding.componentKey,
                rawButtonIds = binding.rawButtonIds,
            ) { client, _ ->
                client.prayerManager.togglePrayer(binding.prayer)
                true
            }
        }
}

