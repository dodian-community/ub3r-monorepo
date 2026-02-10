package net.dodian.jobs.impl;

import net.dodian.uber.game.model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntityProcessorWalkRadiusTest {

    @Test
    public void zeroWalkRadiusAllowsAnyTarget() {
        Position origin = new Position(2600, 3100, 0);
        assertTrue(EntityProcessor.withinWalkRadius(origin, 9999, 9999, 0));
    }

    @Test
    public void targetInsideRadiusIsAllowed() {
        Position origin = new Position(2600, 3100, 0);
        assertTrue(EntityProcessor.withinWalkRadius(origin, 2602, 3098, 2));
    }

    @Test
    public void targetOutsideXRadiusIsRejected() {
        Position origin = new Position(2600, 3100, 0);
        assertFalse(EntityProcessor.withinWalkRadius(origin, 2603, 3100, 2));
    }

    @Test
    public void targetOutsideYRadiusIsRejected() {
        Position origin = new Position(2600, 3100, 0);
        assertFalse(EntityProcessor.withinWalkRadius(origin, 2600, 3097, 2));
    }
}
