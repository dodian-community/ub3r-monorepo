package net.dodian.uber.game.events.item

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.player.Client

/** Fired when a player uses an inventory item on another player. */
data class ItemOnPlayerEvent(
    val client: Client,
    val target: Client,
    val itemId: Int,
    val itemSlot: Int,
) : GameEvent

