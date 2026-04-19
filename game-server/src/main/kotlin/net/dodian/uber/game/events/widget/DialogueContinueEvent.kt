package net.dodian.uber.game.events.widget

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.player.Client

/** Fired when a player clicks the continue button on a dialogue. */
data class DialogueContinueEvent(
    val client: Client,
) : GameEvent

