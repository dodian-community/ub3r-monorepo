package net.dodian.uber.game.runtime.sync.metrics

import java.util.EnumMap
import net.dodian.uber.game.runtime.sync.SynchronizationCycle
import net.dodian.uber.game.runtime.sync.SynchronizationStage
import net.dodian.uber.game.runtime.sync.player.root.PlayerPacketBuildReason
import net.dodian.uber.game.runtime.sync.player.root.PlayerPacketMode
import net.dodian.uber.game.runtime.sync.player.root.PlayerPacketSkipReason
import org.slf4j.Logger

class SynchronizationMetrics(
    private val intervalTicks: Int,
) {
    private val stageSamples = EnumMap<SynchronizationStage, MutableList<Long>>(SynchronizationStage::class.java)
    private val playerPacketModes = EnumMap<PlayerPacketMode, Int>(PlayerPacketMode::class.java)
    private val playerBuildReasons = EnumMap<PlayerPacketBuildReason, Int>(PlayerPacketBuildReason::class.java)
    private val playerSkipReasons = EnumMap<PlayerPacketSkipReason, Int>(PlayerPacketSkipReason::class.java)
    private var tickCount = 0
    private var viewersEncoded = 0
    private var localPlayersWritten = 0
    private var localNpcsWritten = 0
    private var playerAddCount = 0
    private var npcAddCount = 0
    private var playerBlockCacheHits = 0
    private var playerBlockCacheMisses = 0
    private var playerBlockCacheEligible = 0
    private var npcBlockCacheHits = 0
    private var npcBlockCacheMisses = 0
    private var playerPacketsBuilt = 0
    private var playerPacketsSkipped = 0
    private var playerPacketsTemplated = 0
    private var playerScratchReuseCount = 0
    private var playerAppearanceCacheHits = 0
    private var playerAppearanceCacheMisses = 0
    private var playerLocalScans = 0
    private var playerLocalsSkipped = 0
    private var playerTemplatedLocalCoverage = 0
    private var playerSelfOnlyCount = 0
    private var playerIncrementalSteadyCount = 0
    private var playerIncrementalAdmissionCount = 0
    private var playerFullRebuildCount = 0
    private var playerVisibleDiffAdds = 0
    private var playerVisibleDiffRemovals = 0
    private var playerRetainedLocalChangedCount = 0
    private var playerPendingAddCount = 0
    private var playerPendingAddBacklogMax = 0
    private var playerDesiredLocalCount = 0
    private var playerDesiredLocalSaturatedCount = 0
    private var playerLocalRemovalCount = 0
    private var playerLocalAdditionSentCount = 0
    private var playerLocalAdditionDeferredCount = 0
    private var playerHardRebuildRecoveryCount = 0
    private var npcPacketsBuilt = 0
    private var npcPacketsSkipped = 0
    private var npcLocalScans = 0
    private var npcLocalsSkipped = 0

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
        playerBlockCacheEligible += cycle.playerBlockCacheEligible
        npcBlockCacheHits += cycle.npcBlockCacheHits
        npcBlockCacheMisses += cycle.npcBlockCacheMisses
        playerPacketsBuilt += cycle.playerPacketsBuilt
        playerPacketsSkipped += cycle.playerPacketsSkipped
        playerPacketsTemplated += cycle.playerPacketsTemplated
        playerScratchReuseCount += cycle.playerScratchReuseCount
        playerAppearanceCacheHits += cycle.playerAppearanceCacheHits
        playerAppearanceCacheMisses += cycle.playerAppearanceCacheMisses
        playerLocalScans += cycle.playerLocalScans
        playerLocalsSkipped += cycle.playerLocalsSkipped
        playerTemplatedLocalCoverage += cycle.playerTemplatedLocalCoverage
        playerSelfOnlyCount += cycle.playerSelfOnlyCount
        playerIncrementalSteadyCount += cycle.playerIncrementalSteadyCount
        playerIncrementalAdmissionCount += cycle.playerIncrementalAdmissionCount
        playerFullRebuildCount += cycle.playerFullRebuildCount
        playerVisibleDiffAdds += cycle.playerVisibleDiffAdds
        playerVisibleDiffRemovals += cycle.playerVisibleDiffRemovals
        playerRetainedLocalChangedCount += cycle.playerRetainedLocalChangedCount
        playerPendingAddCount += cycle.playerPendingAddCount
        playerPendingAddBacklogMax = maxOf(playerPendingAddBacklogMax, cycle.playerPendingAddBacklogMax)
        playerDesiredLocalCount += cycle.playerDesiredLocalCount
        playerDesiredLocalSaturatedCount += cycle.playerDesiredLocalSaturatedCount
        playerLocalRemovalCount += cycle.playerLocalRemovalCount
        playerLocalAdditionSentCount += cycle.playerLocalAdditionSentCount
        playerLocalAdditionDeferredCount += cycle.playerLocalAdditionDeferredCount
        playerHardRebuildRecoveryCount += cycle.playerHardRebuildRecoveryCount
        cycle.playerPacketModes().forEach { (mode, count) ->
            playerPacketModes[mode] = (playerPacketModes[mode] ?: 0) + count
        }
        cycle.playerBuildReasons().forEach { (reason, count) ->
            playerBuildReasons[reason] = (playerBuildReasons[reason] ?: 0) + count
        }
        cycle.playerSkipReasons().forEach { (reason, count) ->
            playerSkipReasons[reason] = (playerSkipReasons[reason] ?: 0) + count
        }
        npcPacketsBuilt += cycle.npcPacketsBuilt
        npcPacketsSkipped += cycle.npcPacketsSkipped
        npcLocalScans += cycle.npcLocalScans
        npcLocalsSkipped += cycle.npcLocalsSkipped

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
        val modeSummary =
            "K=${playerPacketModes[PlayerPacketMode.SKIP] ?: 0}/${tickCount.coerceAtLeast(1)} " +
                "S=${playerSelfOnlyCount / tickCount.coerceAtLeast(1)} " +
                "IS=${playerIncrementalSteadyCount / tickCount.coerceAtLeast(1)} " +
                "IA=${playerIncrementalAdmissionCount / tickCount.coerceAtLeast(1)} " +
                "F=${playerFullRebuildCount / tickCount.coerceAtLeast(1)}"
        logger.info(
            "[Sync: {}] [Viewers: {}] [Players: {}] [Locals P/N: {}/{}] [Adds P/N: {}/{}] [Player packets B/S/T: {}/{}/{}] [Player modes: {}] [Player locals scan/skip/template: {}/{}/{}] [Player diff A/R/C: {}/{}/{}] [Desired locals avg/sat: {}/{}] [Pending adds avg/max: {}/{}] [Local add sent/deferred: {}/{}] [Removals: {}] [Player reasons B/S: {}/{}] [Recovery rebuilds: {}] [Npc packets B/S: {}/{}] [Npc locals scan/skip: {}/{}] [Block cache hit P/N: {}/{}] [Appearance cache: {}] [Scratch reuse: {}]",
            summary,
            viewersEncoded / tickCount.coerceAtLeast(1),
            onlinePlayers,
            localPlayersWritten / tickCount.coerceAtLeast(1),
            localNpcsWritten / tickCount.coerceAtLeast(1),
            playerAddCount / tickCount.coerceAtLeast(1),
            npcAddCount / tickCount.coerceAtLeast(1),
            playerPacketsBuilt / tickCount.coerceAtLeast(1),
            playerPacketsSkipped / tickCount.coerceAtLeast(1),
            playerPacketsTemplated / tickCount.coerceAtLeast(1),
            modeSummary,
            playerLocalScans / tickCount.coerceAtLeast(1),
            playerLocalsSkipped / tickCount.coerceAtLeast(1),
            playerTemplatedLocalCoverage / tickCount.coerceAtLeast(1),
            playerVisibleDiffAdds / tickCount.coerceAtLeast(1),
            playerVisibleDiffRemovals / tickCount.coerceAtLeast(1),
            playerRetainedLocalChangedCount / tickCount.coerceAtLeast(1),
            playerDesiredLocalCount / tickCount.coerceAtLeast(1),
            playerDesiredLocalSaturatedCount / tickCount.coerceAtLeast(1),
            playerPendingAddCount / tickCount.coerceAtLeast(1),
            playerPendingAddBacklogMax,
            playerLocalAdditionSentCount / tickCount.coerceAtLeast(1),
            playerLocalAdditionDeferredCount / tickCount.coerceAtLeast(1),
            playerLocalRemovalCount / tickCount.coerceAtLeast(1),
            topReasons(playerBuildReasons),
            topReasons(playerSkipReasons),
            playerHardRebuildRecoveryCount / tickCount.coerceAtLeast(1),
            npcPacketsBuilt / tickCount.coerceAtLeast(1),
            npcPacketsSkipped / tickCount.coerceAtLeast(1),
            npcLocalScans / tickCount.coerceAtLeast(1),
            npcLocalsSkipped / tickCount.coerceAtLeast(1),
            hitRate(playerBlockCacheHits, playerBlockCacheMisses, playerBlockCacheEligible),
            hitRate(npcBlockCacheHits, npcBlockCacheMisses),
            hitRate(playerAppearanceCacheHits, playerAppearanceCacheMisses),
            playerScratchReuseCount / tickCount.coerceAtLeast(1),
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
        playerBlockCacheEligible = 0
        npcBlockCacheHits = 0
        npcBlockCacheMisses = 0
        playerPacketsBuilt = 0
        playerPacketsSkipped = 0
        playerPacketsTemplated = 0
        playerScratchReuseCount = 0
        playerAppearanceCacheHits = 0
        playerAppearanceCacheMisses = 0
        playerLocalScans = 0
        playerLocalsSkipped = 0
        playerTemplatedLocalCoverage = 0
        playerSelfOnlyCount = 0
        playerIncrementalSteadyCount = 0
        playerIncrementalAdmissionCount = 0
        playerFullRebuildCount = 0
        playerVisibleDiffAdds = 0
        playerVisibleDiffRemovals = 0
        playerRetainedLocalChangedCount = 0
        playerPendingAddCount = 0
        playerPendingAddBacklogMax = 0
        playerDesiredLocalCount = 0
        playerDesiredLocalSaturatedCount = 0
        playerLocalRemovalCount = 0
        playerLocalAdditionSentCount = 0
        playerLocalAdditionDeferredCount = 0
        playerHardRebuildRecoveryCount = 0
        playerPacketModes.clear()
        playerBuildReasons.clear()
        playerSkipReasons.clear()
        npcPacketsBuilt = 0
        npcPacketsSkipped = 0
        npcLocalScans = 0
        npcLocalsSkipped = 0
    }

    private fun percentile95(samples: List<Long>): Long {
        if (samples.isEmpty()) {
            return 0L
        }
        val sorted = samples.sorted()
        val index = ((sorted.size - 1) * 0.95).toInt()
        return sorted[index]
    }

    private fun hitRate(hits: Int, misses: Int, eligible: Int = hits + misses): String {
        val total = eligible
        if (total <= 0) {
            return "n/a"
        }
        return "${(hits * 100.0 / total).toInt()}%"
    }

    private fun nanosToMillis(nanos: Long): Double = nanos / 1_000_000.0

    private fun format(value: Double): String = String.format("%.2f", value)

    private fun <T> topReasons(values: Map<T, Int>): String {
        if (values.isEmpty()) {
            return "n/a"
        }
        return values.entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString(",") { "${it.key}=${it.value / tickCount.coerceAtLeast(1)}" }
    }
}
