package net.dodian.uber.game.model.entity.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RouteDestinationTest {
    @Test
    void replacesAndConsumesAbsoluteWaypointsInOrder() {
        RouteDestination destination = new RouteDestination();
        MovementPoint first = new MovementPoint(3201, 3200, 0);
        MovementPoint second = new MovementPoint(3205, 3204, 0);

        destination.replace(List.of(first, second), true);

        assertEquals(2, destination.size());
        assertEquals(first, destination.peekFirst());
        assertEquals(second, destination.destination());
        assertTrue(destination.isRunning());
        assertEquals(first, destination.pollFirst());
        assertEquals(second, destination.pollFirst());
        assertTrue(destination.isEmpty());
        assertFalse(destination.isRunning());
    }

    @Test
    void replacementIsAtomicAndSnapshotsCannotMutateTheQueue() {
        RouteDestination destination = new RouteDestination();
        destination.replace(List.of(new MovementPoint(1, 1, 0)), false);
        destination.replace(List.of(new MovementPoint(2, 2, 0)), true);

        assertEquals(List.of(new MovementPoint(2, 2, 0)), destination.snapshot());
        assertThrows(UnsupportedOperationException.class, () ->
                destination.snapshot().add(new MovementPoint(3, 3, 0))
        );
    }

    @Test
    void oversizedRoutesAreRejectedWithoutReplacingTheCurrentRoute() {
        RouteDestination destination = new RouteDestination();
        MovementPoint original = new MovementPoint(1, 1, 0);
        destination.replace(List.of(original), false);
        List<MovementPoint> oversized = new ArrayList<>();
        for (int index = 0; index <= RouteDestination.MAX_WAYPOINTS; index++) {
            oversized.add(new MovementPoint(index, 0, 0));
        }

        assertThrows(IllegalArgumentException.class, () -> destination.replace(oversized, true));
        assertEquals(List.of(original), destination.snapshot());
    }

    @Test
    void legacyArrayAndPointerFieldsAreGone() {
        List<String> fields = java.util.Arrays.stream(Player.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());

        assertFalse(fields.contains("walkingQueueX"));
        assertFalse(fields.contains("walkingQueueY"));
        assertFalse(fields.contains("wQueueReadPtr"));
        assertFalse(fields.contains("wQueueWritePtr"));
        assertFalse(fields.contains("newWalkCmdX"));
        assertFalse(fields.contains("newWalkCmdY"));
        assertFalse(fields.contains("newWalkCmdSteps"));
        assertFalse(fields.contains("travelBackX"));
        assertFalse(fields.contains("travelBackY"));
    }
}
