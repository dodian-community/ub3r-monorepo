package net.dodian.uber.game.runtime.eventbus.events

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.eventbus.GameEvent

data class ObjectClickEvent(
    val client: Client,
    val option: Int,
    val objectId: Int,
    val position: Position,
    val obj: GameObjectData?,
) : GameEvent
