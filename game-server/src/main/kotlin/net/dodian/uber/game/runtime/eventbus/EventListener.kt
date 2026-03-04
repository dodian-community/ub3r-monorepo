package net.dodian.uber.game.runtime.eventbus

class EventListener<E : GameEvent>(
    val condition: (E) -> Boolean = { true },
    val action: (E) -> Boolean,
    val otherwiseAction: (E) -> Unit = {},
)
