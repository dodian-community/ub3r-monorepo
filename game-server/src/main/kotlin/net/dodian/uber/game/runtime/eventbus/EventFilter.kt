package net.dodian.uber.game.runtime.eventbus

fun interface EventFilter<E : GameEvent> {
    fun test(event: E): Boolean
}
