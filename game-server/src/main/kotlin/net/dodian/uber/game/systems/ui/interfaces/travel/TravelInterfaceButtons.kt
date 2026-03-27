package net.dodian.uber.game.systems.ui.interfaces.travel

import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding

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
                    if (client.interactionAnchorX == 2772 && client.interactionAnchorY == 3235) 5
                    else if (client.interactionAnchorX == 2864 && client.interactionAnchorY == 2971) 4
                    else if (client.interactionAnchorX == 3511 && client.interactionAnchorY == 3505) 2
                    else 0
                client.travelTrigger(pos, request.rawButtonId)
                true
            }
        }
}
