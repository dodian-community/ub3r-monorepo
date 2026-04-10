package net.dodian.uber.game.engine.sync.npc

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.chunk.Chunk
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.engine.sync.util.LongLongStampMap

class NpcChunkActivityIndex {
    private val chunkStamps = LongLongStampMap(1024)
    private var sequence = 0L

    fun clear() {
        sequence = 0L
        chunkStamps.clear()
    }

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
        chunkStamps.put(packKey(chunk.x, chunk.y, level), ++sequence)
    }

    fun maxChunkActivityStampFor(viewer: Player, distance: Int): Long {
        val position = viewer.position ?: return 0L
        val centerChunk = position.chunk
        val level = position.z
        val chunkRadius = (distance / Chunk.SIZE) + 2
        var maxStamp = 0L
        for (dx in -chunkRadius until chunkRadius) {
            for (dy in -chunkRadius until chunkRadius) {
                val stamp = chunkStamps.getOrZero(packKey(centerChunk.x + dx, centerChunk.y + dy, level))
                if (stamp > maxStamp) {
                    maxStamp = stamp
                }
            }
        }
        return maxStamp
    }

    private fun packKey(chunkX: Int, chunkY: Int, level: Int): Long {
        // +1 ensures the key is never 0 (reserved as the empty sentinel in the stamp map).
        val x = chunkX.toLong() and 0xFFFF
        val y = chunkY.toLong() and 0xFFFF
        val z = level.toLong() and 0xFFFF
        return (((x shl 32) or (y shl 16) or z) + 1L)
    }
}
