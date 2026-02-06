package net.dodian.uber.game.content.buttons.travel

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

/**
 * Travel menu (interfaceId=802).
 *
 * Button IDs come from the legacy ClickingButtonsListener switch.
 */
object TravelMenuButtons : ButtonContent {
    override val requiredInterfaceId: Int = 802

    override val buttonIds: IntArray = intArrayOf(
        3056,  // Tree
        3057,  // Big tree
        3058,  // Mountain
        3059,  // Castle
        3060,  // Tent
        48054, // Totem
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        val pos =
            if (client.skillX == 2772 && client.skillY == 3235) 5
            else if (client.skillX == 2864 && client.skillY == 2971) 4
            else if (client.skillX == 3511 && client.skillY == 3505) 2
            else 0

        client.travelTrigger(pos)
        return true
    }
}

