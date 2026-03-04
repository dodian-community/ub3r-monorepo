package net.dodian.uber.game.runtime.eventbus.events

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.eventbus.GameEvent

data class DialogueOptionEvent(
    val client: Client,
    val optionIndex: Int,
) : GameEvent
