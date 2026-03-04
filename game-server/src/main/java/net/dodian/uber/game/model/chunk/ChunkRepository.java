package net.dodian.uber.game.model.chunk;

import net.dodian.uber.game.model.EntityType;
import net.dodian.uber.game.model.entity.Entity;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

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
     * Using insertion-ordered sets for low-churn iteration on the game thread.
     */
    private final Map<EntityType, Set<Entity>> entities;

    /**
     * Creates a new repository for the specified chunk.
     *
     * @param chunk The chunk this repository manages
     */
    public ChunkRepository(Chunk chunk) {
        this.chunk = chunk;
        this.entities = new EnumMap<>(EntityType.class);
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
        entities.computeIfAbsent(type, ignored -> new SmallEntitySet<>()).add(entity);
    }

    /**
     * Removes an entity from this chunk repository.
     *
     * @param entity The entity to remove
     */
    public void remove(Entity entity) {
        EntityType type = entity.getEntityType();
        Set<Entity> entitySet = entities.get(type);
        if (entitySet == null) {
            return;
        }
        if (entitySet.remove(entity) && entitySet.isEmpty()) {
            entities.remove(type);
        }
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
        Set<Entity> entitySet = entities.get(type);
        return entitySet == null ? Collections.emptySet() : (Set<E>) entitySet;
    }

    /**
     * Checks if this repository is empty (contains no entities).
     */
    public boolean isEmpty() {
        return entities.isEmpty();
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

    /**
     * Small ordered set optimized for chunk populations that are usually empty
     * or contain only a handful of entities. Promotes to LinkedHashSet only
     * once the per-type population becomes large enough to justify hashing.
     */
    static final class SmallEntitySet<E> extends AbstractSet<E> {
        private static final int PROMOTION_THRESHOLD = 8;

        private E single;
        private ArrayList<E> small;
        private LinkedHashSet<E> large;

        @Override
        public Iterator<E> iterator() {
            if (large != null) {
                return large.iterator();
            }
            if (small != null) {
                return small.iterator();
            }
            if (single == null) {
                return Collections.emptyIterator();
            }
            return new Iterator<>() {
                private boolean hasNext = true;
                private boolean removable;

                @Override
                public boolean hasNext() {
                    return hasNext;
                }

                @Override
                public E next() {
                    if (!hasNext) {
                        throw new NoSuchElementException();
                    }
                    hasNext = false;
                    removable = true;
                    return single;
                }

                @Override
                public void remove() {
                    if (!removable) {
                        throw new IllegalStateException();
                    }
                    SmallEntitySet.this.single = null;
                    removable = false;
                }
            };
        }

        @Override
        public int size() {
            if (large != null) {
                return large.size();
            }
            if (small != null) {
                return small.size();
            }
            return single == null ? 0 : 1;
        }

        @Override
        public boolean contains(Object o) {
            if (large != null) {
                return large.contains(o);
            }
            if (small != null) {
                return small.contains(o);
            }
            return single != null && Objects.equals(single, o);
        }

        @Override
        public boolean add(E entity) {
            if (large != null) {
                return large.add(entity);
            }
            if (single == null && small == null) {
                single = entity;
                return true;
            }
            if (single != null) {
                if (Objects.equals(single, entity)) {
                    return false;
                }
                small = new ArrayList<>(PROMOTION_THRESHOLD);
                small.add(single);
                single = null;
            }
            if (small.contains(entity)) {
                return false;
            }
            small.add(entity);
            if (small.size() > PROMOTION_THRESHOLD) {
                large = new LinkedHashSet<>(small);
                small = null;
            }
            return true;
        }

        @Override
        public boolean remove(Object o) {
            if (large != null) {
                return large.remove(o);
            }
            if (small != null) {
                boolean removed = small.remove(o);
                if (!removed) {
                    return false;
                }
                if (small.isEmpty()) {
                    small = null;
                } else if (small.size() == 1) {
                    single = small.get(0);
                    small = null;
                }
                return true;
            }
            if (single != null && Objects.equals(single, o)) {
                single = null;
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            single = null;
            small = null;
            large = null;
        }
    }
}
