package net.dodian.uber.game.runtime.sync.player

import java.util.HashMap
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.chunk.Chunk
import net.dodian.uber.game.model.entity.player.Player

class PlayerChunkActivityIndex {
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
        val next = ++sequence
        chunkStamps[ChunkLevelKey(chunk.x, chunk.y, level)] = next
    }

    fun snapshotFor(viewer: Player, distance: Int): ViewportActivitySnapshot {
        val position = viewer.position ?: return ViewportActivitySnapshot(0L, 0L)
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
        return ViewportActivitySnapshot(maxStamp, maxStamp)
    }

    private data class ChunkLevelKey(
        val x: Int,
        val y: Int,
        val level: Int,
    )
}
