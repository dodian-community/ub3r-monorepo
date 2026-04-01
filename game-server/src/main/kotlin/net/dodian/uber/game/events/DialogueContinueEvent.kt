package net.dodian.uber.game.events

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.event.GameEvent

data class DialogueContinueEvent(
    val client: Client,
) : GameEvent
