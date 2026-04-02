package net.dodian.uber.game.engine.event

import net.dodian.uber.game.event.GameEvent

fun interface EventFilter<E : GameEvent> {
    fun test(event: E): Boolean
}
