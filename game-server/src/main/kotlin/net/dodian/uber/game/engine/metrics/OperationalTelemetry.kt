package net.dodian.uber.game.engine.metrics

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.LongAdder
import kotlin.math.max

object OperationalTelemetry {
    private const val SAMPLE_CAPACITY = 2048
    private const val TICK_BUDGET_MS = 600L

    private val startedAt = Instant.now().toString()
    private val phaseSamples = ConcurrentHashMap<String, PhaseSamples>()
    private val counters = ConcurrentHashMap<String, LongAdder>()
    private val tickOverruns = LongAdder()
    private val tickCount = LongAdder()
    @Volatile private var lastTickAtMs: Long = 0L

    @JvmStatic
    fun recordPhaseNanos(phase: String, elapsedNs: Long) {
        val samples = phaseSamples.computeIfAbsent(phase) { PhaseSamples(SAMPLE_CAPACITY) }
        samples.record(max(0L, elapsedNs))
    }

    @JvmStatic
    fun recordPhaseMillis(phase: String, elapsedMs: Long) {
        recordPhaseNanos(phase, elapsedMs * 1_000_000L)
    }

    @JvmStatic
    fun getLastPhaseNanos(phase: String): Long = phaseSamples[phase]?.lastValue() ?: 0L

    @JvmStatic
    fun getLastPhaseMillis(phase: String): Long = getLastPhaseNanos(phase) / 1_000_000L

    @JvmStatic
    fun recordTick(elapsedMs: Long, budgetMs: Long = TICK_BUDGET_MS) {
        lastTickAtMs = System.currentTimeMillis()
        tickCount.increment()
        if (elapsedMs > budgetMs) {
            tickOverruns.increment()
            incrementCounter("tick.overrun")
        }
        recordPhaseMillis("tick.total", elapsedMs)
    }

    @JvmStatic
    fun isGameLoopFresh(maxAgeMs: Long = 3_000L): Boolean =
        lastTickAtMs > 0L && System.currentTimeMillis() - lastTickAtMs <= maxAgeMs

    @JvmStatic
    fun gameLoopAgeMs(): Long =
        if (lastTickAtMs <= 0L) Long.MAX_VALUE else (System.currentTimeMillis() - lastTickAtMs).coerceAtLeast(0L)

    @JvmStatic
    fun incrementCounter(name: String, delta: Long = 1L) {
        val adder = counters.computeIfAbsent(name) { LongAdder() }
        if (delta > 0L) {
            adder.add(delta)
        }
    }

    @JvmStatic
    fun snapshot(): Map<String, Any> {
        val phaseStats =
            phaseSamples.entries
                .sortedBy { it.key }
                .associate { it.key to it.value.snapshot() }
        val countersSnapshot =
            counters.entries
                .sortedBy { it.key }
                .associate { it.key to it.value.sum() }

        val overrunCount = tickOverruns.sum()
        val totalTicks = tickCount.sum()
        val overrunRate = if (totalTicks <= 0L) 0.0 else overrunCount.toDouble() / totalTicks.toDouble()
        return linkedMapOf(
            "startedAt" to startedAt,
            "tickBudgetMs" to TICK_BUDGET_MS,
            "tickCount" to totalTicks,
            "tickOverruns" to overrunCount,
            "tickOverrunRate" to overrunRate,
            "lastTickAtMs" to lastTickAtMs,
            "gameLoopAgeMs" to gameLoopAgeMs(),
            "alerts" to mapOf(
                "tickOverrunRateHigh" to (overrunRate >= 0.05),
                "syncSlowEventsHigh" to ((countersSnapshot["sync.slow"] ?: 0L) >= 10L),
                "packetRejectSpike" to ((countersSnapshot["packet.reject.total"] ?: 0L) >= 100L),
            ),
            "phasesMs" to phaseStats,
            "counters" to countersSnapshot,
        )
    }

    private class PhaseSamples(
        capacity: Int,
    ) {
        private val values = LongArray(capacity)
        private val index = AtomicInteger(0)

        fun record(value: Long) {
            val i = index.getAndIncrement()
            values[i % values.size] = value
        }

        fun lastValue(): Long {
            val cur = index.get()
            if (cur <= 0) return 0L
            return values[(cur - 1) % values.size]
        }

        fun snapshot(): Map<String, Any> {
            val upper = minOf(index.get(), values.size)
            if (upper <= 0) {
                return mapOf("count" to 0, "p50" to 0L, "p95" to 0L, "p99" to 0L, "max" to 0L)
            }
            val copy = LongArray(upper)
            for (i in 0 until upper) {
                copy[i] = values[i]
            }
            copy.sort()
            return mapOf(
                "count" to upper,
                "p50" to percentile(copy, 0.50),
                "p95" to percentile(copy, 0.95),
                "p99" to percentile(copy, 0.99),
                "max" to copy.last(),
            )
        }

        private fun percentile(sorted: LongArray, ratio: Double): Long {
            if (sorted.isEmpty()) {
                return 0L
            }
            val pos = ((sorted.size - 1).toDouble() * ratio).toInt().coerceIn(0, sorted.size - 1)
            return sorted[pos]
        }
    }
}
