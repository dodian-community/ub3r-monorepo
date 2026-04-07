package net.dodian.uber.game.events.player

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.player.Client

/** Fired when a player's session is fully initialised and they enter the world. */
data class PlayerLoginEvent(
    val client: Client,
) : GameEvent

