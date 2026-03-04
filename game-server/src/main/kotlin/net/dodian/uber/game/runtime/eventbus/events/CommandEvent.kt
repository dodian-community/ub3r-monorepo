package net.dodian.uber.game.runtime.eventbus.events

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.eventbus.GameEvent

data class CommandEvent(
    val client: Client,
    val rawCommand: String,
    val parsedArgs: List<String>,
) : GameEvent
