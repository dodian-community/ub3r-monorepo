package net.dodian.uber.game.runtime.eventbus

class ReturnableEventListener<E : GameEvent, T>(
    val condition: (E) -> Boolean = { true },
    val action: (E) -> T?,
    val otherwiseAction: (E) -> Unit = {},
)
