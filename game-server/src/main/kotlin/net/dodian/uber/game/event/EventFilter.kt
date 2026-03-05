package net.dodian.uber.game.event

fun interface EventFilter<E : GameEvent> {
    fun test(event: E): Boolean
}
