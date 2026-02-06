package net.dodian.uber.game.content.buttons.partyroom

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.party.Balloons

object PartyRoomDepositButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(8198)

    override fun onClick(client: Client, buttonId: Int): Boolean {
        Balloons.acceptItems(client)
        return true
    }
}
