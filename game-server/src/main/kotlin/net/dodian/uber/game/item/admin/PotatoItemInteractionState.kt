package net.dodian.uber.game.item.admin

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerPotatoState

object PotatoItemInteractionState {
    private const val ITEM_ON_ITEM_FLOW = 4
    private const val ACTIVE_FLAG = 1

    @JvmStatic
    fun beginItemOnItem(client: Client, potatoSlot: Int, otherItemId: Int) {
        client.playerPotatoState = PlayerPotatoState(ITEM_ON_ITEM_FLOW, potatoSlot, otherItemId, ACTIVE_FLAG)
    }
}
