package net.dodian.uber.game.systems.follow

import java.util.concurrent.atomic.AtomicLong
import net.dodian.uber.game.engine.config.runtimePhaseWarnMs
import org.slf4j.LoggerFactory

object FollowPathfindingTelemetry {
    private val logger = LoggerFactory.getLogger(FollowPathfindingTelemetry::class.java)

    private val searchCount = AtomicLong(0L)
    private val failedCount = AtomicLong(0L)
    private val totalNanos = AtomicLong(0L)
    private val maxNanos = AtomicLong(0L)

    @JvmStatic
    fun beginTick() {
        searchCount.set(0L)
        failedCount.set(0L)
        totalNanos.set(0L)
        maxNanos.set(0L)
    }

    @JvmStatic
    fun recordSearch(durationNanos: Long, foundPath: Boolean) {
        searchCount.incrementAndGet()
        if (!foundPath) {
            failedCount.incrementAndGet()
        }
        totalNanos.addAndGet(durationNanos)
        maxNanos.getAndUpdate { current -> if (durationNanos > current) durationNanos else current }
    }

    @JvmStatic
    fun logIfSlow(cycle: Long) {
        val count = searchCount.get()
        if (count <= 0L) {
            return
        }
        val totalMs = totalNanos.get() / 1_000_000L
        val maxMs = maxNanos.get() / 1_000_000L
        if (totalMs < runtimePhaseWarnMs && maxMs < runtimePhaseWarnMs) {
            return
        }
        logger.warn(
            "FOLLOW_PATHING slow: cycle={} searches={} failed={} total={}ms max={}ms avg={}us",
            cycle,
            count,
            failedCount.get(),
            totalMs,
            maxMs,
            (totalNanos.get() / count).coerceAtLeast(0L) / 1_000L,
        )
    }
}
