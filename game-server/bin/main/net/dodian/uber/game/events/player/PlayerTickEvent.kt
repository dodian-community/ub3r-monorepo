package net.dodian.uber.game.events.player

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.player.Client

/** Fired on every game tick for each online player. */
data class PlayerTickEvent(
    val client: Client,
) : GameEvent

