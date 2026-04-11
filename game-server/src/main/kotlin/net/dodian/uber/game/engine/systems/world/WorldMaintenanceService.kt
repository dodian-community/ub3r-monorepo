package net.dodian.uber.game.engine.systems.world

import kotlin.system.measureNanoTime
import net.dodian.uber.game.persistence.world.WorldPollResult
import net.dodian.uber.game.persistence.WorldSavePublisher
import net.dodian.uber.game.persistence.WorldSaveSnapshot
import net.dodian.uber.game.engine.processing.PlunderDoorProcessor
import net.dodian.uber.game.engine.config.gameWorldId
import net.dodian.uber.game.engine.config.runtimePhaseWarnMs
import org.slf4j.LoggerFactory

class WorldMaintenanceService(
    private val plunderDoor: PlunderDoorProcessor,
) {
    private val logger = LoggerFactory.getLogger(WorldMaintenanceService::class.java)
    private val playerIndex = OnlinePlayerIndex()
    private val pollApplier = TargetedWorldPollApplier()
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
        WorldSavePublisher.publish(snapshot)
        worldDbDueCycle = cycle
    }

    fun runWorldDbResultRead(cycle: Long) {
        if (worldDbDueCycle != cycle) {
            return
        }
        pendingWorldPollResult =
            timed(WorldMaintenanceStage.WORLD_DB_RESULT_READ) {
                WorldSavePublisher.latestResult()
            }
    }

    fun runWorldDbApply(cycle: Long) {
        if (worldDbDueCycle != cycle) {
            return
        }
        timed(WorldMaintenanceStage.WORLD_DB_APPLY) {
            pollApplier.apply(pendingWorldPollResult, playerIndex)
        }
        pendingWorldPollResult = WorldPollResult.EMPTY
        worldDbDueCycle = Long.MIN_VALUE
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

    private fun createSnapshot(playerIndex: OnlinePlayerIndex): WorldSaveSnapshot =
        WorldSaveSnapshot(gameWorldId, playerIndex.playerCount(), playerIndex.dbIdsArray())

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
