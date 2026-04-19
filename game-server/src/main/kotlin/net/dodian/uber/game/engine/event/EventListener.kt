package net.dodian.uber.game.engine.event

import net.dodian.uber.game.events.GameEvent

class EventListener<E : GameEvent>(
    val condition: (E) -> Boolean = { true },
    val action: (E) -> Boolean,
    val otherwiseAction: (E) -> Unit = {},
)
