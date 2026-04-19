package net.dodian.uber.game.events.objects

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

/** Fired when a player examines a world object (right-click -> Examine). */
@Suppress("unused")
data class ObjectExamineEvent(
    val client: Client,
    val objectId: Int,
    val position: Position,
) : GameEvent
