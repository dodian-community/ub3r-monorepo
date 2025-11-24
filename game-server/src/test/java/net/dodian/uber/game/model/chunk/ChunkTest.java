package net.dodian.uber.game.model.chunk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChunkTest {

    @Test
    public void testChunkCreation() {
        Chunk chunk = new Chunk(10, 20);
        assertEquals(10, chunk.getX());
        assertEquals(20, chunk.getY());
    }

    @Test
    public void testChunkAbsoluteCoordinates() {
        Chunk chunk = new Chunk(10, 20);
        // Chunk coordinates are relative to origin-6
        // Absolute = SIZE * (coordinate + 6)
        // SIZE = 8, so 8 * (10 + 6) = 128
        assertEquals(128, chunk.getAbsX());
        assertEquals(208, chunk.getAbsY());
    }

    @Test
    public void testChunkEquality() {
        Chunk chunk1 = new Chunk(10, 20);
        Chunk chunk2 = new Chunk(10, 20);
        Chunk chunk3 = new Chunk(11, 20);

        assertEquals(chunk1, chunk2);
        assertNotEquals(chunk1, chunk3);
        assertEquals(chunk1.hashCode(), chunk2.hashCode());
    }

    @Test
    public void testChunkTranslation() {
        Chunk chunk = new Chunk(10, 20);
        Chunk translated = chunk.translate(2, -3);

        assertEquals(12, translated.getX());
        assertEquals(17, translated.getY());
        // Original should not be modified
        assertEquals(10, chunk.getX());
        assertEquals(20, chunk.getY());
    }
}
