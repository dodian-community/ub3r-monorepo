package net.dodian.uber.game.runtime.sync.npc

import java.util.IdentityHashMap
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.chunk.Chunk
import net.dodian.uber.game.model.entity.player.Player

class NpcChunkActivityIndex {
    private val chunkStamps = HashMap<ChunkLevelKey, Long>()
    private var sequence = 0L

    fun bump(position: Position?) {
        if (position == null) {
            return
        }
        bump(position.chunk, position.z)
    }

    fun bump(chunk: Chunk?, level: Int) {
        if (chunk == null) {
            return
        }
        chunkStamps[ChunkLevelKey(chunk.x, chunk.y, level)] = ++sequence
    }

    fun snapshotFor(
        viewer: Player,
        distance: Int,
        localRevisionStamp: Long,
    ): NpcViewportActivitySnapshot {
        val position = viewer.position ?: return NpcViewportActivitySnapshot(0L, localRevisionStamp)
        val centerChunk = position.chunk
        val level = position.z
        val chunkRadius = (distance / Chunk.SIZE) + 2
        var maxStamp = 0L
        for (dx in -chunkRadius until chunkRadius) {
            for (dy in -chunkRadius until chunkRadius) {
                val stamp = chunkStamps[ChunkLevelKey(centerChunk.x + dx, centerChunk.y + dy, level)] ?: 0L
                if (stamp > maxStamp) {
                    maxStamp = stamp
                }
            }
        }
        return NpcViewportActivitySnapshot(maxStamp, localRevisionStamp)
    }

    private data class ChunkLevelKey(
        val x: Int,
        val y: Int,
        val level: Int,
    )
}
