package net.dodian.uber.game.engine.event

import net.dodian.uber.game.events.GameEvent

fun interface EventFilter<E : GameEvent> {
    fun test(event: E): Boolean
}
