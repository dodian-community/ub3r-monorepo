package net.dodian.uber.game.events

import net.dodian.uber.game.events.GameEvent

data class WorldTickEvent(
    val cycle: Int,
) : GameEvent
