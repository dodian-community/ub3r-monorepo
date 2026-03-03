package net.dodian.uber.game.runtime.world

import kotlin.system.measureNanoTime
import net.dodian.jobs.impl.FarmingProcess
import net.dodian.jobs.impl.PlunderDoor
import net.dodian.jobs.impl.WorldProcessor
import net.dodian.uber.game.Server
import net.dodian.uber.game.persistence.WorldDbPollService
import net.dodian.uber.game.persistence.WorldPollInput
import net.dodian.uber.game.persistence.WorldPollResult
import net.dodian.uber.game.runtime.world.farming.FarmingScheduler
import net.dodian.uber.game.runtime.world.metrics.WorldMaintenanceMetrics
import net.dodian.utilities.asyncWorldDbEnabled
import net.dodian.utilities.farmingSchedulerEnabled
import net.dodian.utilities.gameWorldId
import net.dodian.utilities.runtimePhaseWarnMs
import net.dodian.utilities.worldMaintenanceMetricsLogIntervalRuns
import net.dodian.utilities.worldMaintenanceEnabled
import net.dodian.utilities.worldMaintenanceVerboseMetricsEnabled
import org.slf4j.LoggerFactory

class WorldMaintenanceService(
    private val legacyWorldProcessor: WorldProcessor,
    private val legacyFarmingProcess: FarmingProcess,
    private val plunderDoor: PlunderDoor,
) {
    private val logger = LoggerFactory.getLogger(WorldMaintenanceService::class.java)
    private val playerIndex = OnlinePlayerIndex()
    private val pollApplier = TargetedWorldPollApplier()
    private val farmingScheduler = FarmingScheduler.INSTANCE
    private val metrics = WorldMaintenanceMetrics(worldMaintenanceMetricsLogIntervalRuns)
    private var lastPlunderRunMs = 0L

    fun runWorldDb(cycle: Long) {
        if (cycle % MAINTENANCE_INTERVAL_TICKS != 0L) {
            return
        }
        if (!worldMaintenanceEnabled) {
            legacyWorldProcessor.run()
            return
        }
        playerIndex.refresh()
        metrics.recordPlayersIndexed(playerIndex.playerCount())
        val input = timed(WorldMaintenanceStage.WORLD_DB_POLL) {
            createInput(playerIndex)
        }
        val result = timed(WorldMaintenanceStage.WORLD_DB_POLL) {
            poll(input)
        }
        timed(WorldMaintenanceStage.WORLD_DB_APPLY) {
            pollApplier.apply(result, playerIndex)
            Server.chat.clear()
        }
        maybeLogMetrics()
    }

    fun runFarming(cycle: Long) {
        if (cycle % MAINTENANCE_INTERVAL_TICKS != 0L) {
            return
        }
        if (!farmingSchedulerEnabled) {
            legacyFarmingProcess.run()
            return
        }
        playerIndex.refresh()
        farmingScheduler.refreshActivePlayers(playerIndex.snapshot(), cycle)
        timed(WorldMaintenanceStage.FARMING_TICK) {
            val (due, processed) = farmingScheduler.runDue(cycle)
            metrics.recordFarming(due, processed)
        }
        maybeLogMetrics()
    }

    fun runPlunder(nowMs: Long) {
        if (lastPlunderRunMs != 0L && nowMs - lastPlunderRunMs < PLUNDER_DOOR_INTERVAL_MS) {
            return
        }
        timed(WorldMaintenanceStage.PLUNDER_DOOR) {
            plunderDoor.run()
            lastPlunderRunMs = nowMs
        }
        maybeLogMetrics()
    }

    private fun createInput(playerIndex: OnlinePlayerIndex): WorldPollInput =
        WorldPollInput(gameWorldId, playerIndex.playerCount(), playerIndex.dbIds())

    private fun poll(input: WorldPollInput): WorldPollResult {
        return if (asyncWorldDbEnabled) {
            WorldDbPollService.pollAsync(input)
            WorldDbPollService.getLatestResult()
        } else {
            WorldDbPollService.runBlockingPoll(input)
        }
    }

    private fun <T> timed(
        stage: WorldMaintenanceStage,
        block: () -> T,
    ): T {
        var result: T? = null
        val elapsed = measureNanoTime {
            result = block()
        }
        metrics.record(stage, elapsed)
        val elapsedMs = elapsed / 1_000_000L
        if (elapsedMs >= runtimePhaseWarnMs) {
            logger.warn("World maintenance stage {} took {}ms", stage, elapsedMs)
        }
        return result as T
    }

    private fun maybeLogMetrics() {
        if (!worldMaintenanceVerboseMetricsEnabled) {
            return
        }
        metrics.finishRun(logger)
    }

    private companion object {
        private const val MAINTENANCE_INTERVAL_TICKS = 100L
        private const val PLUNDER_DOOR_INTERVAL_MS = 900_000L
    }
}
