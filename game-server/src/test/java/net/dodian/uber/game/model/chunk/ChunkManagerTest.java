package net.dodian.uber.game.model.chunk;

import net.dodian.uber.game.model.EntityType;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class ChunkManagerTest {

    private ChunkManager chunkManager;

    // Mock entity for testing
    private static class MockEntity extends Entity {
        private final EntityType entityType;

        public MockEntity(Position position, EntityType type) {
            super(position, 0, Type.PLAYER);
            this.entityType = type;
        }

        @Override
        public EntityType getEntityType() {
            return entityType;
        }
    }

    @BeforeEach
    public void setup() {
        chunkManager = new ChunkManager();
    }

    @Test
    public void testLoadChunk() {
        Chunk chunk = new Chunk(10, 20);
        ChunkRepository repo = chunkManager.load(chunk);

        assertNotNull(repo);
        assertEquals(chunk, repo.getChunk());
    }

    @Test
    public void testLoadChunkCaching() {
        Chunk chunk = new Chunk(10, 20);
        ChunkRepository repo1 = chunkManager.load(chunk);
        ChunkRepository repo2 = chunkManager.load(chunk);

        // Should return same instance
        assertSame(repo1, repo2);
    }

    @Test
    public void testLoadChunkCachingAcrossEquivalentInstances() {
        ChunkRepository repo1 = chunkManager.load(new Chunk(10, 20));
        ChunkRepository repo2 = chunkManager.load(new Chunk(10, 20));
        assertSame(repo1, repo2);
    }

    @Test
    public void testFindEntitiesInRadius() {
        // Create entity at position (100, 100, 0)
        Position pos = new Position(100, 100, 0);
        MockEntity entity = new MockEntity(pos, EntityType.PLAYER);

        // Add to chunk
        ChunkRepository repo = chunkManager.load(pos.getChunk());
        repo.add(entity);

        // Find entities within 16 tiles
        Set<MockEntity> found = chunkManager.find(pos, EntityType.PLAYER, 16);

        assertTrue(found.contains(entity));
        assertEquals(1, found.size());
    }

    @Test
    public void testFindEntitiesMultipleChunks() {
        // Create entities in different chunks
        Position pos1 = new Position(100, 100, 0);
        MockEntity entity1 = new MockEntity(pos1, EntityType.PLAYER);

        Position pos2 = new Position(108, 100, 0); // 8 tiles away
        MockEntity entity2 = new MockEntity(pos2, EntityType.PLAYER);

        // Add to their respective chunks
        chunkManager.load(pos1.getChunk()).add(entity1);
        chunkManager.load(pos2.getChunk()).add(entity2);

        // Find all entities within 16 tiles of entity1
        Set<MockEntity> found = chunkManager.find(pos1, EntityType.PLAYER, 16);

        assertEquals(2, found.size());
        assertTrue(found.contains(entity1));
        assertTrue(found.contains(entity2));
    }

    @Test
    public void testFindDoesNotCreateRepositoriesForEmptyChunks() {
        assertEquals(0, chunkManager.getLoadedChunkCount());
        Set<MockEntity> found = chunkManager.find(new Position(3200, 3200, 0), EntityType.PLAYER, 16);
        assertTrue(found.isEmpty());
        assertEquals(0, chunkManager.getLoadedChunkCount());
    }
}
