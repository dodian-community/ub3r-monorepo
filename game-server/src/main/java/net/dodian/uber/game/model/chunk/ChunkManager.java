package net.dodian.uber.game.model.chunk;

import net.dodian.uber.game.model.EntityType;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Manages all chunks in the game world.
 * Provides spatial queries for finding entities near a position.
 * Thread-safe for concurrent access.
 */
public final class ChunkManager {

    /**
     * Map of chunk to its repository.
     */
    private final Map<Chunk, ChunkRepository> chunks = new ConcurrentHashMap<>();

    /**
     * Loads or creates a chunk repository for the specified chunk.
     * Returns cached instance if already loaded.
     *
     * @param chunk The chunk to load
     * @return The chunk repository
     */
    public ChunkRepository load(Chunk chunk) {
        return chunks.computeIfAbsent(chunk, key -> new ChunkRepository(chunk));
    }

    /**
     * Gets the repository for an already-loaded chunk.
     *
     * @param chunk The chunk key
     * @return The repository, or null if not loaded
     */
    public ChunkRepository getLoaded(Chunk chunk) {
        return chunks.get(chunk);
    }

    /**
     * Finds all entities of a specific type within a radius of a position.
     *
     * @param center The center position
     * @param type The entity type to find
     * @param distance The search radius in tiles
     * @param <E> The entity class type
     * @return Set of entities within range
     */
    public <E extends Entity> Set<E> find(Position center, EntityType type, int distance) {
        return find(center, type, distance, HashSet::new, entity -> true);
    }

    /**
     * Finds entities using a caller-provided output set and predicate.
     */
    @SuppressWarnings("unchecked")
    public <E extends Entity> Set<E> find(Position center,
                                          EntityType type,
                                          int distance,
                                          Supplier<Set<E>> setFactory,
                                          Predicate<E> predicate) {
        Set<E> found = setFactory.get();

        // Calculate chunk radius needed
        // Each chunk is 8 tiles, so divide distance by 8 and add 2 for safety
        int chunkRadius = (distance / Chunk.SIZE) + 2;

        Chunk centerChunk = center.getChunk();

        // Scan all chunks in the radius. Query path does not create chunks.
        for (int dx = -chunkRadius; dx < chunkRadius; dx++) {
            for (int dy = -chunkRadius; dy < chunkRadius; dy++) {
                Chunk targetChunk = centerChunk.translate(dx, dy);
                ChunkRepository repo = getLoaded(targetChunk);
                if (repo == null) {
                    continue;
                }

                // Get all entities of this type in the chunk
                Set<E> entities = repo.getAll(type);

                // Filter by actual distance
                for (E entity : entities) {
                    Position entityPos = entity.getPosition();
                    if (entityPos != null
                            && center.withinDistance(entityPos, distance)
                            && predicate.test(entity)) {
                        found.add(entity);
                    }
                }
            }
        }

        return found;
    }

    /**
     * Finds local players for a viewer with Luna-style prioritization.
     */
    public Set<Player> findUpdatePlayers(Player viewer, int distance) {
        Supplier<Set<Player>> setFactory = () -> {
            if (viewer.playerListSize > 50) {
                return new TreeSet<>(new ChunkPlayerComparator(viewer));
            }
            return new HashSet<>();
        };

        return find(
                viewer.getPosition(),
                EntityType.PLAYER,
                distance,
                setFactory,
                other -> other != null && other.isActive && other != viewer
        );
    }

    /**
     * Gets the number of loaded chunks.
     */
    public int getLoadedChunkCount() {
        return chunks.size();
    }

    /**
     * Clears all loaded chunks. Use with caution.
     */
    public void clear() {
        chunks.clear();
    }
}
