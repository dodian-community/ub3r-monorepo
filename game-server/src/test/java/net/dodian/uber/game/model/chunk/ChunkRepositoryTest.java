package net.dodian.uber.game.model.chunk;

import net.dodian.uber.game.model.EntityType;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class ChunkRepositoryTest {

    private ChunkRepository repository;
    private Chunk chunk;

    // Mock entity for testing
    private static class MockEntity extends Entity {
        private final EntityType entityType;

        public MockEntity(EntityType type) {
            super(new Position(100, 100, 0), 0, Type.PLAYER);
            this.entityType = type;
        }

        @Override
        public EntityType getEntityType() {
            return entityType;
        }
    }

    @BeforeEach
    public void setup() {
        chunk = new Chunk(10, 20);
        repository = new ChunkRepository(chunk);
    }

    @Test
    public void testAddEntity() {
        Entity entity = new MockEntity(EntityType.PLAYER);
        repository.add(entity);

        Set<Entity> entities = repository.getAll(EntityType.PLAYER);
        assertTrue(entities.contains(entity));
        assertEquals(1, entities.size());
    }

    @Test
    public void testRemoveEntity() {
        Entity entity = new MockEntity(EntityType.PLAYER);
        repository.add(entity);
        repository.remove(entity);

        Set<Entity> entities = repository.getAll(EntityType.PLAYER);
        assertFalse(entities.contains(entity));
        assertEquals(0, entities.size());
    }

    @Test
    public void testGetAllByType() {
        Entity entity1 = new MockEntity(EntityType.PLAYER);
        Entity entity2 = new MockEntity(EntityType.PLAYER);

        repository.add(entity1);
        repository.add(entity2);

        Set<Entity> entities = repository.getAll(EntityType.PLAYER);
        assertEquals(2, entities.size());
        assertTrue(entities.contains(entity1));
        assertTrue(entities.contains(entity2));
    }

    @Test
    public void testEntityTypeSegregation() {
        // Add entities of different types
        Entity player = new MockEntity(EntityType.PLAYER);
        Entity npc = new MockEntity(EntityType.NPC);

        repository.add(player);
        repository.add(npc);

        // Players should be in PLAYER set
        Set<Entity> players = repository.getAll(EntityType.PLAYER);
        assertEquals(1, players.size());
        assertTrue(players.contains(player));

        // NPCs should be in NPC set
        Set<Entity> npcs = repository.getAll(EntityType.NPC);
        assertEquals(1, npcs.size());
        assertTrue(npcs.contains(npc));
    }
}
