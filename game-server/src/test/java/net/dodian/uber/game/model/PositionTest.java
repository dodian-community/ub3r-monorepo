package net.dodian.uber.game.model;

import net.dodian.uber.game.model.chunk.Chunk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PositionTest {

    @Test
    void computesChunkValuesOnDemand() {
        Position position = new Position(2611, 3093, 0);

        assertEquals(320, position.getChunkX());
        assertEquals(380, position.getChunkY());

        Chunk chunk = position.getChunk();
        assertEquals(320, chunk.getX());
        assertEquals(380, chunk.getY());
    }

    @Test
    void storesCoordinatesInNarrowPrimitivesWithoutChangingPublicValues() {
        Position position = new Position(3200, 3200, 3);

        position.move(5, -7, 1);

        assertEquals(3205, position.getX());
        assertEquals(3193, position.getY());
        assertEquals(4, position.getZ());
    }

    @Test
    void rejectsCoordinateOverflow() {
        assertThrows(IllegalArgumentException.class, () -> new Position(Short.MAX_VALUE + 1, 3200, 0));
        assertThrows(IllegalArgumentException.class, () -> new Position(3200, Short.MIN_VALUE - 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Position(3200, 3200, Byte.MAX_VALUE + 1));
    }
}
