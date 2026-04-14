package net.dodian.uber.game.events.objects

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

/** Fired when a player clicks an option on a world object. [option] is 1-based. */
data class ObjectClickEvent(
    val client: Client,
    val option: Int,
    val objectId: Int,
    val position: Position,
    val obj: GameObjectData?,
) : GameEvent

