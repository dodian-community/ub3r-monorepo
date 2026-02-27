package net.dodian.uber.game.model.chunk;

import net.dodian.uber.game.model.EntityType;
import net.dodian.uber.game.model.entity.Entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository for storing entities within a single chunk.
 * Entities are segregated by type for efficient lookups.
 * Thread-safe for concurrent access.
 */
public final class ChunkRepository {

    /**
     * The chunk this repository manages.
     */
    private final Chunk chunk;

    /**
     * Map of entity type to set of entities.
     * Using ConcurrentHashMap for thread-safety.
     */
    private final Map<EntityType, Set<Entity>> entities;

    /**
     * Creates a new repository for the specified chunk.
     *
     * @param chunk The chunk this repository manages
     */
    public ChunkRepository(Chunk chunk) {
        this.chunk = chunk;
        this.entities = new ConcurrentHashMap<>();

        // Initialize empty sets for each entity type
        for (EntityType type : EntityType.values()) {
            entities.put(type, ConcurrentHashMap.newKeySet());
        }
    }

    /**
     * Gets the chunk this repository manages.
     */
    public Chunk getChunk() {
        return chunk;
    }

    /**
     * Adds an entity to this chunk repository.
     *
     * @param entity The entity to add
     */
    public void add(Entity entity) {
        EntityType type = entity.getEntityType();
        entities.get(type).add(entity);
    }

    /**
     * Removes an entity from this chunk repository.
     *
     * @param entity The entity to remove
     */
    public void remove(Entity entity) {
        EntityType type = entity.getEntityType();
        entities.get(type).remove(entity);
    }

    /**
     * Gets all entities of the specified type in this chunk.
     *
     * @param type The entity type
     * @param <E> The entity class type
     * @return Set of entities
     */
    @SuppressWarnings("unchecked")
    public <E extends Entity> Set<E> getAll(EntityType type) {
        return (Set<E>) entities.get(type);
    }

    /**
     * Checks if this repository is empty (contains no entities).
     */
    public boolean isEmpty() {
        for (Set<Entity> entitySet : entities.values()) {
            if (!entitySet.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the total count of entities in this chunk.
     */
    public int size() {
        int total = 0;
        for (Set<Entity> entitySet : entities.values()) {
            total += entitySet.size();
        }
        return total;
    }
}
