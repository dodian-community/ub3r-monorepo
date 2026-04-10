package net.dodian.uber.game.events.item

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

/** Fired when a player uses an inventory item on a world object. */
data class ItemOnObjectEvent(
    val client: Client,
    val objectId: Int,
    val position: Position,
    val obj: GameObjectData?,
    val itemId: Int,
    val itemSlot: Int,
    val interfaceId: Int,
) : GameEvent

