package net.dodian.uber.game.runtime.sync.viewport

import java.util.IdentityHashMap
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.EntityType
import net.dodian.uber.game.model.chunk.Chunk
import net.dodian.uber.game.model.chunk.ChunkRepository
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.runtime.sync.util.LongObjectMap

class ViewportIndex private constructor(
    private val snapshots: IdentityHashMap<Player, ViewportSnapshot>,
) {
    fun snapshotFor(viewer: Player): ViewportSnapshot? = snapshots[viewer]

    companion object {
        fun build(viewers: List<Client>, distance: Int): ViewportIndex? {
            val chunkManager = Server.chunkManager ?: return null
            val neighborhoodCache = LongObjectMap<ViewportSnapshot>(viewers.size.coerceAtLeast(16))
            val snapshots = IdentityHashMap<Player, ViewportSnapshot>(viewers.size)
            viewers.forEach { viewer ->
                val position = viewer.position ?: return@forEach
                val centerChunkX = position.chunkX
                val centerChunkY = position.chunkY
                val neighborhoodKey = pack(centerChunkX, centerChunkY, position.z)
                val snapshot =
                    neighborhoodCache.getOrPut(neighborhoodKey) {
                        buildNeighborhood(chunkManager, centerChunkX, centerChunkY, position.z, distance)
                    }
                snapshots[viewer] = snapshot
            }
            return ViewportIndex(snapshots)
        }

        private fun buildNeighborhood(
            chunkManager: net.dodian.uber.game.model.chunk.ChunkManager,
            centerChunkX: Int,
            centerChunkY: Int,
            level: Int,
            distance: Int,
        ): ViewportSnapshot {
            val chunkRadius = (distance / Chunk.SIZE) + 2
            val players = ArrayList<Player>()
            val npcs = ArrayList<Npc>()
            for (dx in -chunkRadius until chunkRadius) {
                for (dy in -chunkRadius until chunkRadius) {
                    val repo: ChunkRepository = chunkManager.getLoaded(centerChunkX + dx, centerChunkY + dy) ?: continue
                    for (other in repo.getAll<Player>(EntityType.PLAYER)) {
                        if (other != null && other.isActive && other.position?.z == level) {
                            players += other
                        }
                    }
                    for (npc in repo.getAll<Npc>(EntityType.NPC)) {
                        if (npc != null && npc.isVisible && npc.position?.z == level) {
                            npcs += npc
                        }
                    }
                }
            }
            return ViewportSnapshot(players, npcs)
        }

        private fun pack(
            chunkX: Int,
            chunkY: Int,
            level: Int,
        ): Long = (((chunkX.toLong() and 0x1fffffL) shl 43) or ((chunkY.toLong() and 0x1fffffL) shl 22) or (level.toLong() and 0x3fffffL))
    }
}
