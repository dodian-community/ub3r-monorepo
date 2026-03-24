package net.dodian.uber.game.content.interfaces.travel

import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

object TravelInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        TravelComponents.options.map { option ->
            buttonBinding(
                interfaceId = TravelComponents.INTERFACE_ID,
                componentId = option.componentId,
                componentKey = option.componentKey,
                rawButtonIds = option.rawButtonIds,
                requiredInterfaceId = TravelComponents.INTERFACE_ID,
            ) { client, request ->
                val pos =
                    if (client.skillX == 2772 && client.skillY == 3235) 5
                    else if (client.skillX == 2864 && client.skillY == 2971) 4
                    else if (client.skillX == 3511 && client.skillY == 3505) 2
                    else 0
                client.travelTrigger(pos, request.rawButtonId)
                true
            }
        }
}

