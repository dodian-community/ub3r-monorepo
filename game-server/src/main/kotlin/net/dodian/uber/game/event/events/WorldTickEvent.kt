package net.dodian.uber.game.event.events

import net.dodian.uber.game.event.GameEvent

data class WorldTickEvent(
    val cycle: Int,
) : GameEvent
