package net.dodian.uber.game.content.ui

import net.dodian.uber.game.Server
import net.dodian.uber.game.content.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.content.ui.buttons.buttonBinding

object SlotsInterface : InterfaceButtonContent {
    private val spinButtons = intArrayOf(54074)

    override val bindings =
        listOf(
            buttonBinding(-1, 0, "slots.spin", spinButtons) { client, _ ->
                Server.slots.playSlots(client, -1)
                true
            },
        )
}
