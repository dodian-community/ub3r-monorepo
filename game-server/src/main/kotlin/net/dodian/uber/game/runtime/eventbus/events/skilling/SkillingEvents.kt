package net.dodian.uber.game.runtime.eventbus.events.skilling

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.eventbus.GameEvent
import net.dodian.uber.game.skills.core.ActionStopReason

data class SkillingActionStartedEvent(
    val client: Client,
    val actionName: String,
) : GameEvent

data class SkillingActionCycleEvent(
    val client: Client,
    val actionName: String,
) : GameEvent

data class SkillingActionSucceededEvent(
    val client: Client,
    val actionName: String,
) : GameEvent

data class SkillingActionStoppedEvent(
    val client: Client,
    val actionName: String,
    val reason: ActionStopReason,
) : GameEvent
