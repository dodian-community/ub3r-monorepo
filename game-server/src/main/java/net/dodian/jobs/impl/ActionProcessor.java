package net.dodian.jobs.impl;

import net.dodian.uber.game.runtime.queue.QueueTaskService;
import net.dodian.uber.game.event.GameEventBus;
import net.dodian.uber.game.event.events.WorldTickEvent;
import net.dodian.uber.game.model.entity.player.PlayerHandler;

/**
 * Processes queued game actions/events in tick-order.
 */
public class ActionProcessor implements Runnable {
    @Override
    public void run() {
        QueueTaskService.processDue();
        GameEventBus.INSTANCE.post(new WorldTickEvent(PlayerHandler.cycle));
    }
}
