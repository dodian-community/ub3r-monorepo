package net.dodian.jobs.impl;

import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.runtime.eventbus.GameEventBus;
import net.dodian.uber.game.runtime.eventbus.events.WorldTickEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import kotlin.Unit;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionProcessorTest {

    @AfterEach
    void tearDown() {
        GameEventBus.clear();
    }

    @Test
    void postsWorldTickEventDuringRun() {
        AtomicInteger observedCycle = new AtomicInteger(-1);
        PlayerHandler.cycle = 123;
        GameEventBus.on(WorldTickEvent.class, new net.dodian.uber.game.runtime.eventbus.EventListener<>(
                event -> true,
                event -> {
                    observedCycle.set(event.getCycle());
                    return false;
                },
                event -> Unit.INSTANCE
        ));

        new ActionProcessor().run();

        assertEquals(123, observedCycle.get());
    }
}
