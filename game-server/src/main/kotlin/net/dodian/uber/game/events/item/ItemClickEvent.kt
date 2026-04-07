package net.dodian.uber.game.events.item

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.player.Client

/** Fired when a player clicks an item in their inventory or a container interface. */
data class ItemClickEvent(
    val client: Client,
    val itemId: Int,
    val itemSlot: Int,
    val interfaceId: Int,
) : GameEvent

