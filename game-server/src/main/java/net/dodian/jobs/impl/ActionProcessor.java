package net.dodian.jobs.impl;

import net.dodian.uber.game.event.EventManager;

/**
 * Processes queued game actions/events in tick-order.
 */
public class ActionProcessor implements Runnable {
    @Override
    public void run() {
        EventManager.getInstance().processEvents();
    }
}
