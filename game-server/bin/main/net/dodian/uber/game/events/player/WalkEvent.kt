package net.dodian.uber.game.events.player

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.Position

/**
 * Fired when a player begins walking or running to a destination.
 * [destination] is the target tile the client requested.
 * Useful for movement hooks, anti-cheat, and wilderness entry detection.
 */
data class WalkEvent(
    val client: Client,
    val destination: Position,
) : GameEvent

