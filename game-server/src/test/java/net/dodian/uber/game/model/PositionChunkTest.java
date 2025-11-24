package net.dodian.uber.game.model;

import net.dodian.uber.game.model.chunk.Chunk;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PositionChunkTest {

    @Test
    public void testGetChunk() {
        // Position at world coords (100, 150, 0)
        Position pos = new Position(100, 150, 0);
        Chunk chunk = pos.getChunk();

        // Chunk X = (100 / 8) - 6 = 12 - 6 = 6
        // Chunk Y = (150 / 8) - 6 = 18 - 6 = 12
        assertEquals(6, chunk.getX());
        assertEquals(12, chunk.getY());
    }

    @Test
    public void testGetChunkBottomLeftCorner() {
        // Test at exact chunk boundary
        Position pos = new Position(128, 208, 0);
        Chunk chunk = pos.getChunk();

        // 128 / 8 = 16, 16 - 6 = 10
        // 208 / 8 = 26, 26 - 6 = 20
        assertEquals(10, chunk.getX());
        assertEquals(20, chunk.getY());
    }

    @Test
    public void testGetChunkConsistency() {
        // Positions within same chunk should return equal chunks
        // Chunk (6, 12) contains world coords 96-103 (X) and 144-151 (Y)
        Position pos1 = new Position(100, 150, 0);
        Position pos2 = new Position(102, 148, 0);  // Same chunk

        assertEquals(pos1.getChunk(), pos2.getChunk());
    }

    @Test
    public void testGetChunkDifferentHeights() {
        // Same X,Y but different Z should still return same chunk
        Position pos1 = new Position(100, 150, 0);
        Position pos2 = new Position(100, 150, 1);

        assertEquals(pos1.getChunk(), pos2.getChunk());
    }
}
