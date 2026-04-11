package net.dodian.uber.game.model.chunk

import java.util.HashSet
import java.util.LinkedHashSet
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.Supplier
import net.dodian.uber.game.model.EntityType
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Player

class ChunkManager {
    private val chunks = ConcurrentHashMap<Long, ChunkRepository>()

    fun load(chunk: Chunk): ChunkRepository =
        chunks.computeIfAbsent(pack(chunk.x, chunk.y)) { ChunkRepository(chunk) }

    fun getLoaded(chunk: Chunk): ChunkRepository? = chunks[pack(chunk.x, chunk.y)]

    fun getLoaded(chunkX: Int, chunkY: Int): ChunkRepository? = chunks[pack(chunkX, chunkY)]

    fun <E : Entity> find(center: Position, type: EntityType, distance: Int): MutableSet<E> {
        return find(center, type, distance, Supplier { HashSet<E>() }, Predicate { true })
    }

    @Suppress("UNCHECKED_CAST")
    fun <E : Entity> find(
        center: Position,
        type: EntityType,
        distance: Int,
        setFactory: Supplier<MutableSet<E>>,
        predicate: Predicate<E>,
    ): MutableSet<E> {
        val found = setFactory.get()
        val chunkRadius = (distance / Chunk.SIZE) + 2
        val centerChunkX = center.chunkX
        val centerChunkY = center.chunkY

        for (dx in -chunkRadius until chunkRadius) {
            for (dy in -chunkRadius until chunkRadius) {
                val repo = getLoaded(centerChunkX + dx, centerChunkY + dy) ?: continue
                val entities = repo.getAll<E>(type)
                for (entity in entities) {
                    val entityPos = entity.position
                    if (entityPos != null && center.withinDistance(entityPos, distance) && predicate.test(entity)) {
                        found.add(entity)
                    }
                }
            }
        }

        return found
    }

    fun findUpdatePlayers(viewer: Player, distance: Int): MutableSet<Player> {
        return find(
            viewer.position,
            EntityType.PLAYER,
            distance,
            Supplier { LinkedHashSet<Player>() },
            Predicate { other -> other.isActive && other !== viewer },
        )
    }

    fun findUpdateNpcs(viewer: Player, distance: Int): MutableSet<Npc> {
        return find(
            viewer.position,
            EntityType.NPC,
            distance,
            Supplier { HashSet<Npc>() },
            Predicate { npc -> npc.isVisible },
        )
    }

    fun forEachViewableChunk(center: Position, distance: Int, consumer: Consumer<ChunkRepository>) {
        val chunkRadius = (distance / Chunk.SIZE) + 2
        val centerChunkX = center.chunkX
        val centerChunkY = center.chunkY

        for (dx in -chunkRadius until chunkRadius) {
            for (dy in -chunkRadius until chunkRadius) {
                val repo = getLoaded(centerChunkX + dx, centerChunkY + dy) ?: continue
                consumer.accept(repo)
            }
        }
    }

    fun forEachUpdatePlayerCandidate(viewer: Player, distance: Int, consumer: Consumer<Player>) {
        forEach(
            viewer.position,
            EntityType.PLAYER,
            distance,
            Predicate { other -> other.isActive && other !== viewer },
            consumer,
        )
    }

    fun forEachUpdateNpcCandidate(viewer: Player, distance: Int, consumer: Consumer<Npc>) {
        forEach(
            viewer.position,
            EntityType.NPC,
            distance,
            Predicate { npc -> npc.isVisible },
            consumer,
        )
    }

    private fun <E : Entity> forEach(
        center: Position,
        type: EntityType,
        distance: Int,
        predicate: Predicate<E>,
        consumer: Consumer<E>,
    ) {
        val chunkRadius = (distance / Chunk.SIZE) + 2
        val centerChunkX = center.chunkX
        val centerChunkY = center.chunkY

        for (dx in -chunkRadius until chunkRadius) {
            for (dy in -chunkRadius until chunkRadius) {
                val repo = getLoaded(centerChunkX + dx, centerChunkY + dy) ?: continue
                val entities = repo.getAll<E>(type)
                for (entity in entities) {
                    val entityPos = entity.position
                    if (entityPos != null && center.withinDistance(entityPos, distance) && predicate.test(entity)) {
                        consumer.accept(entity)
                    }
                }
            }
        }
    }

    fun getLoadedChunkCount(): Int = chunks.size

    fun clear() {
        chunks.clear()
    }

    private companion object {
        private fun pack(x: Int, y: Int): Long = (x.toLong() shl 32) xor (y.toLong() and 0xffffffffL)
    }
}
