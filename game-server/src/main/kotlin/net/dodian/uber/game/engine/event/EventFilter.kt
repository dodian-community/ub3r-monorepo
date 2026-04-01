package net.dodian.uber.game.engine.event

fun interface EventFilter<E : GameEvent> {
    fun test(event: E): Boolean
}
