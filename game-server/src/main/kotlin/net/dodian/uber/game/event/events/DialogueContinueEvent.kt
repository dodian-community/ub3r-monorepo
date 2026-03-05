package net.dodian.uber.game.event.events

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.event.GameEvent

data class DialogueContinueEvent(
    val client: Client,
) : GameEvent
