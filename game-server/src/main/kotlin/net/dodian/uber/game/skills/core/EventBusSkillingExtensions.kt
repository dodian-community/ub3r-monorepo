package net.dodian.uber.game.skills.core

import net.dodian.uber.game.runtime.eventbus.GameEvent
import net.dodian.uber.game.runtime.eventbus.GameEventBus

inline fun <reified E : GameEvent> onSkillingEvent(
    noinline condition: (E) -> Boolean = { true },
    noinline action: (E) -> Boolean,
) {
    GameEventBus.on(E::class.java, net.dodian.uber.game.runtime.eventbus.EventListener(condition, action, {}))
}
