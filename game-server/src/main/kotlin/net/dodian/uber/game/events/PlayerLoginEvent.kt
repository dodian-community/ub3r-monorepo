package net.dodian.uber.game.events

import net.dodian.uber.game.event.GameEvent
import net.dodian.uber.game.model.entity.player.Client

data class PlayerLoginEvent(
    val client: Client,
) : GameEvent
