package net.dodian.uber.game.content.interfaces.slots

import net.dodian.uber.game.Server
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

object SlotsInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        listOf(
            buttonBinding(-1, 0, "slots.spin", SlotsComponents.spinButtons) { client, _ ->
                Server.slots.playSlots(client, -1)
                true
            }
        )
}
