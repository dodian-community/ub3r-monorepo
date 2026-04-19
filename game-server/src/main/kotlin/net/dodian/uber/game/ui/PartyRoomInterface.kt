package net.dodian.uber.game.ui

import net.dodian.uber.game.activity.partyroom.PartyRoomBalloons
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

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
                PartyRoomBalloons.acceptOfferedPartyItems(client)
                true
            },
        )
}
