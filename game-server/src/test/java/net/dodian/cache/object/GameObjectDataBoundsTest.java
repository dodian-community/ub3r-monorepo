package net.dodian.cache.object;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GameObjectDataBoundsTest {

    @Test
    public void acceptsDefinitionAtOldUpperBound() {
        GameObjectData def = new GameObjectData(32_538, "test", "", 1, 1, false, false, false, true, 2);
        assertDoesNotThrow(() -> GameObjectData.addDefinition(def));
        assertNotNull(GameObjectData.forId(32_538));
    }

    @Test
    public void dynamicallyExpandsForHigherDefinitionIds() {
        GameObjectData def = new GameObjectData(40_000, "test-high", "", 1, 1, false, false, false, true, 2);
        assertDoesNotThrow(() -> GameObjectData.addDefinition(def));
        assertNotNull(GameObjectData.forId(40_000));
    }
}
