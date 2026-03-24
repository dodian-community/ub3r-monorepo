package net.dodian.uber.game.skills.core.events

import net.dodian.uber.game.event.GameEvent
import net.dodian.uber.game.event.GameEventBus

inline fun <reified E : GameEvent> onSkillingEvent(
    noinline condition: (E) -> Boolean = { true },
    noinline action: (E) -> Boolean,
) {
    GameEventBus.on(E::class.java, net.dodian.uber.game.event.EventListener(condition, action, {}))
}
