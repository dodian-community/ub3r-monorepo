package net.dodian.uber.game.runtime.eventbus.events

import net.dodian.uber.game.runtime.eventbus.GameEvent

data class WorldTickEvent(
    val cycle: Int,
) : GameEvent
