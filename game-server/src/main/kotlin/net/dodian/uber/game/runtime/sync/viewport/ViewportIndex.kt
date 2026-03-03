package net.dodian.uber.game.runtime.sync.viewport

import java.util.IdentityHashMap
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.EntityType
import net.dodian.uber.game.model.chunk.Chunk
import net.dodian.uber.game.model.chunk.ChunkRepository
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player

class ViewportIndex private constructor(
    private val snapshots: IdentityHashMap<Player, ViewportSnapshot>,
) {
    fun snapshotFor(viewer: Player): ViewportSnapshot? = snapshots[viewer]

    companion object {
        fun build(viewers: List<Client>, distance: Int): ViewportIndex? {
            val chunkManager = Server.chunkManager ?: return null
            val neighborhoodCache = HashMap<ChunkKey, ChunkSyncSnapshot>()
            val snapshots = IdentityHashMap<Player, ViewportSnapshot>(viewers.size)
            viewers.forEach { viewer ->
                val position = viewer.position ?: return@forEach
                val key = ChunkKey(position.chunk, position.z)
                val neighborhood =
                    neighborhoodCache.computeIfAbsent(key) {
                        ChunkSyncSnapshot(buildNeighborhood(chunkManager, key.chunk, key.level, distance))
                    }.neighborhood
                snapshots[viewer] = ViewportSnapshot(neighborhood.players, neighborhood.npcs)
            }
            return ViewportIndex(snapshots)
        }

        private fun buildNeighborhood(
            chunkManager: net.dodian.uber.game.model.chunk.ChunkManager,
            centerChunk: Chunk,
            level: Int,
            distance: Int,
        ): ChunkNeighborhoodSnapshot {
            val chunkRadius = (distance / Chunk.SIZE) + 2
            val players = LinkedHashSet<Player>()
            val npcs = LinkedHashSet<Npc>()
            for (dx in -chunkRadius until chunkRadius) {
                for (dy in -chunkRadius until chunkRadius) {
                    val repo: ChunkRepository = chunkManager.getLoaded(centerChunk.translate(dx, dy)) ?: continue
                    repo.getAll<Player>(EntityType.PLAYER)
                        .filterTo(players) { other ->
                            other != null && other.isActive && other.position?.z == level
                        }
                    repo.getAll<Npc>(EntityType.NPC)
                        .filterTo(npcs) { npc ->
                            npc != null && npc.isVisible && npc.position?.z == level
                        }
                }
            }
            return ChunkNeighborhoodSnapshot(players.toList(), npcs.toList())
        }

        private data class ChunkKey(
            val chunk: Chunk,
            val level: Int,
        )
    }
}
