package net.dodian.uber.game.content.skills.core.events

import net.dodian.uber.game.content.skills.core.runtime.ActionStopReason
import net.dodian.uber.game.event.GameEvent
import net.dodian.uber.game.event.GameEventBus
import net.dodian.uber.game.event.events.skilling.SkillingActionStartedEvent
import net.dodian.uber.game.event.events.skilling.SkillingActionStoppedEvent

inline fun <reified E : GameEvent> onSkillingEvent(
    noinline condition: (E) -> Boolean = { true },
    noinline action: (E) -> Boolean,
) {
    GameEventBus.on(E::class.java, net.dodian.uber.game.event.EventListener(condition, action, {}))
}

fun onSkillStart(
    actionName: String,
    action: (SkillingActionStartedEvent) -> Boolean,
) {
    onSkillingEvent<SkillingActionStartedEvent>(
        condition = { it.actionName.equals(actionName, ignoreCase = true) },
        action = action,
    )
}

fun onSkillStop(
    actionName: String,
    stopReason: ActionStopReason? = null,
    action: (SkillingActionStoppedEvent) -> Boolean,
) {
    onSkillingEvent<SkillingActionStoppedEvent>(
        condition = {
            it.actionName.equals(actionName, ignoreCase = true) &&
                (stopReason == null || it.reason == stopReason)
        },
        action = action,
    )
}
