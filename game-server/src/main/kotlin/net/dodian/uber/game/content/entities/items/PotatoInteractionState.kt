package net.dodian.uber.game.content.entities.items

import net.dodian.uber.game.model.entity.player.Client

object PotatoInteractionState {
    private const val ITEM_ON_ITEM_FLOW = 4
    private const val ACTIVE_FLAG = 1

    @JvmStatic
    fun beginItemOnItem(client: Client, potatoSlot: Int, otherItemId: Int) {
        client.playerPotato.clear()
        client.playerPotato.add(0, ITEM_ON_ITEM_FLOW)
        client.playerPotato.add(1, potatoSlot)
        client.playerPotato.add(2, otherItemId)
        client.playerPotato.add(3, ACTIVE_FLAG)
    }
}
