package net.dodian.uber.game.engine.event

import net.dodian.uber.game.events.GameEvent

class ReturnableEventListener<E : GameEvent, T>(
    val condition: (E) -> Boolean = { true },
    val action: (E) -> T?,
    val otherwiseAction: (E) -> Unit = {},
)
