package net.dodian.uber.game.events

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.event.GameEvent

data class CommandEvent(
    val client: Client,
    val rawCommand: String,
    val parsedArgs: List<String>,
) : GameEvent
