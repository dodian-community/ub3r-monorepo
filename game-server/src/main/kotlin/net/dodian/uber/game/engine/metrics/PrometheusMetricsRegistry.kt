package net.dodian.uber.game.engine.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.Histogram
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import java.io.StringWriter
import java.util.concurrent.ConcurrentLinkedQueue
import net.dodian.uber.game.engine.config.metricsEnabled
import net.dodian.uber.game.engine.config.metricsPrometheusEndpoint
import net.dodian.uber.game.engine.config.metricsDashboardEnabled

data class MetricSnapshot(
    val timestamp: Long,
    val players: Int,
    val npcs: Int,
    val cycleTimeMs: Double,
    val playerCycleMs: Double,
    val npcCycleMs: Double,
    val worldCycleMs: Double = 0.0,
    val heapMemoryMb: Double,
    val maxMemoryMb: Double,
    val bandwidthInKb: Double,
    val bandwidthOutKb: Double,
    val loginSuccess: Long,
    val loginFailed: Long,
)

object PrometheusMetricsRegistry {
    private val registry: CollectorRegistry = CollectorRegistry.defaultRegistry

    val activePlayers: Gauge = Gauge.build()
        .name("dodian_active_players")
        .help("Number of currently online players.")
        .register(registry)

    val activeNpcs: Gauge = Gauge.build()
        .name("dodian_active_npcs")
        .help("Number of currently spawned NPCs.")
        .register(registry)

    val cycleTimeMs: Histogram = Histogram.build()
        .name("dodian_cycle_time_ms")
        .help("Overall game tick processing duration in milliseconds.")
        .buckets(0.1, 0.5, 1.0, 5.0, 10.0, 20.0, 50.0, 100.0, 200.0, 400.0, 600.0)
        .register(registry)

    val playerCycleMs: Histogram = Histogram.build()
        .name("dodian_cycle_player_ms")
        .help("Player processing duration per tick in milliseconds.")
        .buckets(0.1, 0.5, 1.0, 5.0, 10.0, 20.0, 50.0, 100.0, 200.0)
        .register(registry)

    val npcCycleMs: Histogram = Histogram.build()
        .name("dodian_cycle_npc_ms")
        .help("NPC processing duration per tick in milliseconds.")
        .buckets(0.1, 0.5, 1.0, 5.0, 10.0, 20.0, 50.0, 100.0, 200.0)
        .register(registry)

    val worldCycleMs: Histogram = Histogram.build()
        .name("dodian_cycle_world_ms")
        .help("World processing duration per tick in milliseconds.")
        .buckets(0.1, 0.5, 1.0, 5.0, 10.0, 20.0, 50.0, 100.0, 200.0)
        .register(registry)

    val bandwidthInBytes: Counter = Counter.build()
        .name("dodian_cycle_bandwidth_in_bytes")
        .help("Total incoming network traffic in bytes.")
        .register(registry)

    val bandwidthOutBytes: Counter = Counter.build()
        .name("dodian_cycle_bandwidth_out_bytes")
        .help("Total outgoing network traffic in bytes.")
        .register(registry)

    val loginAttempts: Counter = Counter.build()
        .name("dodian_login_attempts_total")
        .help("Total login attempts by status outcome.")
        .labelNames("status")
        .register(registry)

    private val history = ConcurrentLinkedQueue<MetricSnapshot>()
    private const val MAX_HISTORY_SIZE = 720 // 1 hour at 5s sampling rate

    private var lastRecordedBandwidthIn = 0L
    private var lastRecordedBandwidthOut = 0L
    private var lastRecordedTimestamp = System.currentTimeMillis()

    init {
        try {
            DefaultExports.initialize()
        } catch (_: Exception) {
        }
    }

    fun recordLogin(status: String) {
        if (!metricsEnabled) return
        loginAttempts.labels(status).inc()
    }

    fun recordTickMetrics(
        players: Int,
        npcs: Int,
        totalCycleMs: Double,
        playerMs: Double,
        npcMs: Double,
        worldMs: Double = 0.0,
        bytesInTotal: Long = 0L,
        bytesOutTotal: Long = 0L
    ) {
        if (!metricsEnabled) return

        activePlayers.set(players.toDouble())
        activeNpcs.set(npcs.toDouble())
        cycleTimeMs.observe(totalCycleMs)
        playerCycleMs.observe(playerMs)
        npcCycleMs.observe(npcMs)
        worldCycleMs.observe(worldMs)

        val now = System.currentTimeMillis()
        val deltaSec = ((now - lastRecordedTimestamp) / 1000.0).coerceAtLeast(0.001)

        val deltaInBytes = (bytesInTotal - lastRecordedBandwidthIn).coerceAtLeast(0)
        val deltaOutBytes = (bytesOutTotal - lastRecordedBandwidthOut).coerceAtLeast(0)

        if (deltaInBytes > 0) bandwidthInBytes.inc(deltaInBytes.toDouble())
        if (deltaOutBytes > 0) bandwidthOutBytes.inc(deltaOutBytes.toDouble())

        val runtime = Runtime.getRuntime()
        val heapUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0)
        val heapMaxMb = runtime.maxMemory() / (1024.0 * 1024.0)

        val kbInPerSec = (deltaInBytes / 1024.0) / deltaSec
        val kbOutPerSec = (deltaOutBytes / 1024.0) / deltaSec

        val loginSuccessCount = loginAttempts.labels("success").get().toLong()
        val loginFailedCount = loginAttempts.labels("failed").get().toLong()

        history.add(
            MetricSnapshot(
                timestamp = now,
                players = players,
                npcs = npcs,
                cycleTimeMs = totalCycleMs,
                playerCycleMs = playerMs,
                npcCycleMs = npcMs,
                worldCycleMs = worldMs,
                heapMemoryMb = heapUsedMb,
                maxMemoryMb = heapMaxMb,
                bandwidthInKb = kbInPerSec,
                bandwidthOutKb = kbOutPerSec,
                loginSuccess = loginSuccessCount,
                loginFailed = loginFailedCount
            )
        )

        while (history.size > MAX_HISTORY_SIZE) {
            history.poll()
        }

        lastRecordedBandwidthIn = bytesInTotal
        lastRecordedBandwidthOut = bytesOutTotal
        lastRecordedTimestamp = now
    }

    fun getPrometheusTextFormat(): String {
        if (!metricsEnabled || !metricsPrometheusEndpoint) {
            return "# Prometheus metrics disabled in Settings.toml\n"
        }
        val writer = StringWriter()
        TextFormat.write004(writer, registry.metricFamilySamples())
        return writer.toString()
    }

    fun getHistorySnapshot(): List<MetricSnapshot> {
        return history.toList()
    }
}
