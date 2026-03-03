package net.dodian.uber.game.runtime.sync.metrics

import java.util.EnumMap
import net.dodian.uber.game.runtime.sync.SynchronizationCycle
import net.dodian.uber.game.runtime.sync.SynchronizationStage
import org.slf4j.Logger

class SynchronizationMetrics(
    private val intervalTicks: Int,
) {
    private val stageSamples = EnumMap<SynchronizationStage, MutableList<Long>>(SynchronizationStage::class.java)
    private var tickCount = 0
    private var viewersEncoded = 0
    private var localPlayersWritten = 0
    private var localNpcsWritten = 0
    private var playerAddCount = 0
    private var npcAddCount = 0
    private var playerBlockCacheHits = 0
    private var playerBlockCacheMisses = 0
    private var npcBlockCacheHits = 0
    private var npcBlockCacheMisses = 0

    fun record(cycle: SynchronizationCycle, onlinePlayers: Int, logger: Logger) {
        tickCount++
        SynchronizationStage.values().forEach { stage ->
            stageSamples.computeIfAbsent(stage) { ArrayList(intervalTicks) }.add(cycle.stageDurationNanos(stage))
        }
        viewersEncoded += cycle.viewersEncoded
        localPlayersWritten += cycle.localPlayersWritten
        localNpcsWritten += cycle.localNpcsWritten
        playerAddCount += cycle.playerAddCount
        npcAddCount += cycle.npcAddCount
        playerBlockCacheHits += cycle.playerBlockCacheHits
        playerBlockCacheMisses += cycle.playerBlockCacheMisses
        npcBlockCacheHits += cycle.npcBlockCacheHits
        npcBlockCacheMisses += cycle.npcBlockCacheMisses

        if (tickCount < intervalTicks) {
            return
        }

        val summary =
            SynchronizationStage.values().joinToString(" ") { stage ->
                val samples = stageSamples[stage].orEmpty()
                val averageMs = samples.map { nanosToMillis(it) }.average().takeIf { !it.isNaN() } ?: 0.0
                val p95Ms = nanosToMillis(percentile95(samples))
                "${stage.name}=${format(averageMs)}/${format(p95Ms)}ms"
            }
        logger.info(
            "[Sync: {}] [Viewers: {}] [Players: {}] [Locals P/N: {}/{}] [Adds P/N: {}/{}] [Block cache hit P/N: {}/{}]",
            summary,
            viewersEncoded / tickCount.coerceAtLeast(1),
            onlinePlayers,
            localPlayersWritten / tickCount.coerceAtLeast(1),
            localNpcsWritten / tickCount.coerceAtLeast(1),
            playerAddCount / tickCount.coerceAtLeast(1),
            npcAddCount / tickCount.coerceAtLeast(1),
            hitRate(playerBlockCacheHits, playerBlockCacheMisses),
            hitRate(npcBlockCacheHits, npcBlockCacheMisses),
        )
        reset()
    }

    private fun reset() {
        tickCount = 0
        stageSamples.clear()
        viewersEncoded = 0
        localPlayersWritten = 0
        localNpcsWritten = 0
        playerAddCount = 0
        npcAddCount = 0
        playerBlockCacheHits = 0
        playerBlockCacheMisses = 0
        npcBlockCacheHits = 0
        npcBlockCacheMisses = 0
    }

    private fun percentile95(samples: List<Long>): Long {
        if (samples.isEmpty()) {
            return 0L
        }
        val sorted = samples.sorted()
        val index = ((sorted.size - 1) * 0.95).toInt()
        return sorted[index]
    }

    private fun hitRate(hits: Int, misses: Int): String {
        val total = hits + misses
        if (total <= 0) {
            return "n/a"
        }
        return "${(hits * 100.0 / total).toInt()}%"
    }

    private fun nanosToMillis(nanos: Long): Double = nanos / 1_000_000.0

    private fun format(value: Double): String = String.format("%.2f", value)
}
