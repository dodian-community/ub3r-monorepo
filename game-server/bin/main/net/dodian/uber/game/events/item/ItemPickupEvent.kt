package net.dodian.uber.game.events.item

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GroundItem
import net.dodian.uber.game.model.Position

/** Fired when a player picks up a ground item. */
data class ItemPickupEvent(
    val client: Client,
    val item: GroundItem,
    val position: Position,
) : GameEvent

