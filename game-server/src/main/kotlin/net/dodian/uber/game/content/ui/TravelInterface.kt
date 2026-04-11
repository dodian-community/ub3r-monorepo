package net.dodian.uber.game.content.ui

import net.dodian.uber.game.content.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.content.ui.buttons.buttonBinding

object TravelInterface : InterfaceButtonContent {
    private const val INTERFACE_ID = 802

    private data class TravelOption(
        val componentId: Int,
        val componentKey: String,
        val rawButtonIds: IntArray,
    )

    private val options =
        listOf(
            TravelOption(0, "travel.tree", intArrayOf(3056)),
            TravelOption(1, "travel.big_tree", intArrayOf(3057)),
            TravelOption(2, "travel.mountain", intArrayOf(3058)),
            TravelOption(3, "travel.castle", intArrayOf(3059)),
            TravelOption(4, "travel.tent", intArrayOf(3060)),
            TravelOption(5, "travel.totem", intArrayOf(48054)),
        )

    override val bindings =
        options.map { option ->
            buttonBinding(
                interfaceId = INTERFACE_ID,
                componentId = option.componentId,
                componentKey = option.componentKey,
                rawButtonIds = option.rawButtonIds,
                requiredInterfaceId = INTERFACE_ID,
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
