package net.dodian.uber.game.content.buttons.crafting

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client

object LeatherCraftButtons : ButtonContent {
    private val hideCraftButtons = intArrayOf(
        34185, 34184, 34183, 34182,
        34189, 34188, 34187, 34186,
        34193, 34192, 34191, 34190,
    )

    private val normalCraftButtons = intArrayOf(
        33187, 33186, 33185,
        33190, 33189, 33188,
        33193, 33192, 33191,
        33196, 33195, 33194,
        33199, 33198, 33197,
        33202, 33201, 33200,
        33205, 33204, 33203,
    )

    override val buttonIds: IntArray = hideCraftButtons + normalCraftButtons

    override fun onClick(client: Client, buttonId: Int): Boolean {
        if (buttonId in hideCraftButtons) {
            client.startHideCraft(buttonId)
            return true
        }
        if (buttonId in normalCraftButtons) {
            client.startCraft(buttonId)
            return true
        }
        return false
    }
}

