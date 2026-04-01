package net.dodian.uber.game.events

import net.dodian.uber.game.engine.event.GameEvent

data class WorldTickEvent(
    val cycle: Int,
) : GameEvent
