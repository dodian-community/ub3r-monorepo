package net.dodian.jobs;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GameTickSchedulerTest {

    @Test
    public void deterministicOrderAndWallClockIntervals() {
        GameTickScheduler scheduler = new GameTickScheduler(600);
        List<String> runs = new ArrayList<>();

        scheduler.registerTask("entity", 600, () -> runs.add("entity"));
        scheduler.registerTask("world", 60_000, () -> runs.add("world"));
        scheduler.registerTask("item", 600, () -> runs.add("item"));

        scheduler.runTickForTesting(0);
        scheduler.runTickForTesting(600);
        scheduler.runTickForTesting(1_200);
        scheduler.runTickForTesting(60_000);

        assertEquals(
                List.of(
                        "entity", "world", "item",
                        "entity", "item",
                        "entity", "item",
                        "entity", "world", "item"
                ),
                runs
        );
    }
}
