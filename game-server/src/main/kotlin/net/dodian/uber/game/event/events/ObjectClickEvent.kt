package net.dodian.uber.game.event.events

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.event.GameEvent

data class ObjectClickEvent(
    val client: Client,
    val option: Int,
    val objectId: Int,
    val position: Position,
    val obj: GameObjectData?,
) : GameEvent
