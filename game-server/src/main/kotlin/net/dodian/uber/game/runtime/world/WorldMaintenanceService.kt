package net.dodian.uber.game.runtime.world

import kotlin.system.measureNanoTime
import net.dodian.jobs.impl.FarmingProcess
import net.dodian.jobs.impl.PlunderDoor
import net.dodian.uber.game.Server
import net.dodian.uber.game.persistence.world.WorldPollResult
import net.dodian.uber.game.persistence.WorldPollPublisher
import net.dodian.uber.game.persistence.WorldPollSnapshot
import net.dodian.uber.game.runtime.world.farming.FarmingScheduler
import net.dodian.utilities.farmingSchedulerEnabled
import net.dodian.utilities.gameWorldId
import net.dodian.utilities.runtimePhaseWarnMs
import org.slf4j.LoggerFactory

class WorldMaintenanceService(
    private val legacyFarmingProcess: FarmingProcess,
    private val plunderDoor: PlunderDoor,
) {
    private val logger = LoggerFactory.getLogger(WorldMaintenanceService::class.java)
    private val playerIndex = OnlinePlayerIndex()
    private val pollApplier = TargetedWorldPollApplier()
    private val farmingScheduler = FarmingScheduler.INSTANCE
    private var lastPlunderRunMs = 0L
    private var worldDbDueCycle = Long.MIN_VALUE
    private var pendingWorldPollResult: WorldPollResult = WorldPollResult.EMPTY

    fun runWorldDbInputBuild(cycle: Long) {
        if (!isMaintenanceDue(cycle)) {
            return
        }
        playerIndex.refresh()
        val snapshot = timed(WorldMaintenanceStage.WORLD_DB_INPUT_BUILD) {
            createSnapshot(playerIndex)
        }
        WorldPollPublisher.publish(snapshot)
        worldDbDueCycle = cycle
    }

    fun runWorldDbResultRead(cycle: Long) {
        if (worldDbDueCycle != cycle) {
            return
        }
        pendingWorldPollResult =
            timed(WorldMaintenanceStage.WORLD_DB_RESULT_READ) {
                WorldPollPublisher.latestResult()
            }
    }

    fun runWorldDbApply(cycle: Long) {
        if (worldDbDueCycle != cycle) {
            return
        }
        timed(WorldMaintenanceStage.WORLD_DB_APPLY) {
            pollApplier.apply(pendingWorldPollResult, playerIndex)
            Server.chat.clear()
        }
        pendingWorldPollResult = WorldPollResult.EMPTY
        worldDbDueCycle = Long.MIN_VALUE
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
            farmingScheduler.runDue(cycle)
        }
    }

    fun runPlunder(nowMs: Long) {
        if (lastPlunderRunMs != 0L && nowMs - lastPlunderRunMs < PLUNDER_DOOR_INTERVAL_MS) {
            return
        }
        timed(WorldMaintenanceStage.PLUNDER_DOOR) {
            plunderDoor.run()
            lastPlunderRunMs = nowMs
        }
    }

    private fun createSnapshot(playerIndex: OnlinePlayerIndex): WorldPollSnapshot =
        WorldPollSnapshot(gameWorldId, playerIndex.playerCount(), playerIndex.dbIdsArray())

    @Suppress("UNCHECKED_CAST", "VARIABLE_WITH_REDUNDANT_INITIALIZER")
    private fun <T> timed(
        stage: WorldMaintenanceStage,
        block: () -> T,
    ): T {
        var result: Any? = null
        val elapsed = measureNanoTime {
            result = block()
        }
        val elapsedMs = elapsed / 1_000_000L
        if (elapsedMs >= runtimePhaseWarnMs) {
            logger.warn("World maintenance stage {} took {}ms", stage, elapsedMs)
        }
        return result as T
    }

    private companion object {
        private const val MAINTENANCE_INTERVAL_TICKS = 100L
        private const val PLUNDER_DOOR_INTERVAL_MS = 900_000L
    }

    private fun isMaintenanceDue(cycle: Long): Boolean = cycle % MAINTENANCE_INTERVAL_TICKS == 0L
}
