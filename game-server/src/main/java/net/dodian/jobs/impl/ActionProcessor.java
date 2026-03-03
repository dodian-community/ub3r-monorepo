package net.dodian.jobs.impl;

import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.runtime.queue.QueueTaskService;

/**
 * Processes queued game actions/events in tick-order.
 */
public class ActionProcessor implements Runnable {
    @Override
    public void run() {
        QueueTaskService.processDue();
        EventManager.getInstance().processEvents();
    }
}
