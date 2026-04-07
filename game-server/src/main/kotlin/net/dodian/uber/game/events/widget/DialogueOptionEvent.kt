package net.dodian.uber.game.events.widget

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.player.Client

/** Fired when a player selects an option from a dialogue menu. */
data class DialogueOptionEvent(
    val client: Client,
    val optionIndex: Int,
) : GameEvent

