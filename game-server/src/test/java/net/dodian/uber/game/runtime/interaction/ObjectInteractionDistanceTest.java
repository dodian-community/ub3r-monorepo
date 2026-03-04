package net.dodian.uber.game.runtime.interaction;

import io.netty.channel.embedded.EmbeddedChannel;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ObjectInteractionDistanceTest {

    @Test
    void miningModeBlocksFarDistanceClicks() {
        Client client = clientAt(2600, 3100, 0);
        WalkToTask task = new WalkToTask(WalkToTask.Action.OBJECT_FIRST_CLICK, 7456, new Position(2604, 3100, 0));

        Position result = ObjectInteractionDistance.resolveDistancePosition(
                client,
                task,
                7456,
                null,
                null,
                ObjectInteractionDistance.DistanceMode.MINING
        );

        Assertions.assertNull(result);
    }

    @Test
    void miningModeAllowsAdjacentClicks() {
        Client client = clientAt(2603, 3100, 0);
        WalkToTask task = new WalkToTask(WalkToTask.Action.OBJECT_FIRST_CLICK, 7456, new Position(2604, 3100, 0));

        Position result = ObjectInteractionDistance.resolveDistancePosition(
                client,
                task,
                7456,
                null,
                null,
                ObjectInteractionDistance.DistanceMode.MINING
        );

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2604, result.getX());
        Assertions.assertEquals(3100, result.getY());
        Assertions.assertEquals(0, result.getZ());
    }

    @Test
    void miningModeBlocksDiagonalAdjacency() {
        Client client = clientAt(2603, 3099, 0);
        WalkToTask task = new WalkToTask(WalkToTask.Action.OBJECT_FIRST_CLICK, 7456, new Position(2604, 3100, 0));

        Position result = ObjectInteractionDistance.resolveDistancePosition(
                client,
                task,
                7456,
                null,
                null,
                ObjectInteractionDistance.DistanceMode.MINING
        );

        Assertions.assertNull(result);
    }

    @Test
    void genericClickModeStillAllowsDistanceBoundOverrideBehavior() {
        Client client = clientAt(2603, 3100, 0);
        WalkToTask task = new WalkToTask(WalkToTask.Action.OBJECT_FIRST_CLICK, 11643, new Position(2605, 3100, 0));

        Position result = ObjectInteractionDistance.resolveDistancePosition(
                client,
                task,
                11643,
                null,
                null,
                ObjectInteractionDistance.DistanceMode.CLICK
        );

        Assertions.assertNotNull(result);
    }

    private static Client clientAt(int x, int y, int z) {
        Client client = new Client(new EmbeddedChannel(), 1);
        client.getPosition().moveTo(x, y, z);
        client.isActive = true;
        client.disconnected = false;
        return client;
    }
}
