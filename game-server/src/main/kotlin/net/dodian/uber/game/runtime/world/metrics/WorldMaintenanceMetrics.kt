package net.dodian.uber.game.runtime.world.metrics

import java.util.EnumMap
import net.dodian.uber.game.runtime.world.WorldMaintenanceStage
import org.slf4j.Logger

class WorldMaintenanceMetrics(
    private val intervalRuns: Int,
) {
    private val stageSamples = EnumMap<WorldMaintenanceStage, MutableList<Long>>(WorldMaintenanceStage::class.java)
    private var runCount = 0
    private var playersIndexed = 0
    private var dueFarmers = 0
    private var processedFarmers = 0

    fun record(
        stage: WorldMaintenanceStage,
        durationNanos: Long,
    ) {
        stageSamples.computeIfAbsent(stage) { ArrayList(intervalRuns.coerceAtLeast(1)) }.add(durationNanos)
    }

    fun recordPlayersIndexed(count: Int) {
        playersIndexed += count
    }

    fun recordFarming(due: Int, processed: Int) {
        dueFarmers += due
        processedFarmers += processed
    }

    fun finishRun(logger: Logger) {
        runCount++
        if (runCount < intervalRuns.coerceAtLeast(1)) {
            return
        }
        val summary =
            WorldMaintenanceStage.values().joinToString(" ") { stage ->
                val samples = stageSamples[stage].orEmpty()
                val averageMs = samples.map { it / 1_000_000.0 }.average().takeIf { !it.isNaN() } ?: 0.0
                "${stage.name}=${"%.2f".format(averageMs)}ms"
            }
        logger.info(
            "[WorldMaintenance: {}] [Indexed players: {}] [Farming due/processed: {}/{}]",
            summary,
            playersIndexed / runCount.coerceAtLeast(1),
            dueFarmers / runCount.coerceAtLeast(1),
            processedFarmers / runCount.coerceAtLeast(1),
        )
        stageSamples.clear()
        runCount = 0
        playersIndexed = 0
        dueFarmers = 0
        processedFarmers = 0
    }
}
