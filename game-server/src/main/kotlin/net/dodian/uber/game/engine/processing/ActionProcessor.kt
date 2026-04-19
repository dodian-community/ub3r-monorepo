package net.dodian.uber.game.engine.processing

import net.dodian.uber.game.engine.scheduler.QueueTaskService
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.WorldTickEvent
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry

/**
 * Processes queued game actions/events in tick-order.
 */
class ActionProcessor : Runnable {
    override fun run() {
        QueueTaskService.processDue()
        GameEventBus.post(WorldTickEvent(PlayerRegistry.cycle))
    }
}
