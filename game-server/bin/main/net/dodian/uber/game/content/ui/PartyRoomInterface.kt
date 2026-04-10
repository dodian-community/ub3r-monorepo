package net.dodian.uber.game.content.ui

import net.dodian.uber.game.content.events.partyroom.Balloons
import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding

object PartyRoomInterface : InterfaceButtonContent {
    private val depositAcceptButtons = intArrayOf(8198)

    override val bindings =
        listOf(
            buttonBinding(
                interfaceId = -1,
                componentId = 0,
                componentKey = "partyroom.deposit.accept",
                rawButtonIds = depositAcceptButtons,
            ) { client, _ ->
                Balloons.acceptItems(client)
                true
            },
        )
}
