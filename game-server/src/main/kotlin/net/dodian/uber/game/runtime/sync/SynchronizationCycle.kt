package net.dodian.uber.game.runtime.sync

import java.util.EnumMap
import net.dodian.uber.game.runtime.sync.cache.RootSynchronizationCache
import net.dodian.uber.game.runtime.sync.viewport.ViewportIndex

class SynchronizationCycle(
    val tick: Long,
    val rootCache: RootSynchronizationCache,
    val viewportIndex: ViewportIndex?,
) {
    private val stageDurationsNanos = EnumMap<SynchronizationStage, Long>(SynchronizationStage::class.java)

    var viewersEncoded: Int = 0
        private set
    var localPlayersWritten: Int = 0
        private set
    var localNpcsWritten: Int = 0
        private set
    var playerAddCount: Int = 0
        private set
    var npcAddCount: Int = 0
        private set
    var playerBlockCacheHits: Int = 0
        private set
    var playerBlockCacheMisses: Int = 0
        private set
    var npcBlockCacheHits: Int = 0
        private set
    var npcBlockCacheMisses: Int = 0
        private set

    fun recordStage(stage: SynchronizationStage, durationNanos: Long) {
        stageDurationsNanos[stage] = (stageDurationsNanos[stage] ?: 0L) + durationNanos
    }

    fun stageDurationNanos(stage: SynchronizationStage): Long = stageDurationsNanos[stage] ?: 0L

    fun recordViewer(localPlayers: Int, localNpcs: Int) {
        viewersEncoded++
        localPlayersWritten += localPlayers
        localNpcsWritten += localNpcs
    }

    fun recordPlayerAdd() {
        playerAddCount++
    }

    fun recordNpcAdd() {
        npcAddCount++
    }

    fun recordPlayerBlockCacheHit(hit: Boolean) {
        if (hit) {
            playerBlockCacheHits++
        } else {
            playerBlockCacheMisses++
        }
    }

    fun recordNpcBlockCacheHit(hit: Boolean) {
        if (hit) {
            npcBlockCacheHits++
        } else {
            npcBlockCacheMisses++
        }
    }
}
