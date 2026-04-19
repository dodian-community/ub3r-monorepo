package net.dodian.uber.game.events.item

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

/** Fired when a player drops an item from their inventory onto the ground. */
data class ItemDropEvent(
    val client: Client,
    val itemId: Int,
    val slot: Int,
    val amount: Int,
    val position: Position,
) : GameEvent
