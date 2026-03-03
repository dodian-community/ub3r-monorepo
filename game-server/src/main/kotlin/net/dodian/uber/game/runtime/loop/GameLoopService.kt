package net.dodian.uber.game.runtime.loop

import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureNanoTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dodian.jobs.impl.ActionProcessor
import net.dodian.jobs.impl.EntityProcessor
import net.dodian.jobs.impl.FarmingProcess
import net.dodian.jobs.impl.ItemProcessor
import net.dodian.jobs.impl.ObjectProcess
import net.dodian.jobs.impl.OutboundPacketProcessor
import net.dodian.jobs.impl.PlunderDoor
import net.dodian.jobs.impl.ShopProcessor
import net.dodian.jobs.impl.WorldProcessor
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.runtime.metrics.TickPhaseTimer
import net.dodian.uber.game.runtime.process.InboundPacketPhase
import net.dodian.uber.game.runtime.process.LegacyActionPhase
import net.dodian.uber.game.runtime.process.MovementFinalizePhase
import net.dodian.uber.game.runtime.process.NpcMainPhase
import net.dodian.uber.game.runtime.process.OutboundSyncPhase
import net.dodian.uber.game.runtime.process.PlayerMainPhase
import net.dodian.uber.game.runtime.process.WorldMaintenancePhase
import org.slf4j.LoggerFactory
import net.dodian.utilities.runtimeCycleLogEnabled
import net.dodian.utilities.runtimeCycleLogIntervalTicks
import net.dodian.utilities.runtimePhaseTimingEnabled
import net.dodian.utilities.runtimePhaseWarnMs

class GameLoopService(
    private val entityProcessor: EntityProcessor = EntityProcessor(),
    private val actionProcessor: ActionProcessor = ActionProcessor(),
    private val outboundPacketProcessor: OutboundPacketProcessor = OutboundPacketProcessor(),
    private val itemProcessor: ItemProcessor = ItemProcessor(),
    private val shopProcessor: ShopProcessor = ShopProcessor(),
    private val objectProcess: ObjectProcess = ObjectProcess(),
    private val worldProcessor: WorldProcessor = WorldProcessor(),
    private val farmingProcess: FarmingProcess = FarmingProcess(),
    private val plunderDoor: PlunderDoor = PlunderDoor(),
) {
    private val logger = LoggerFactory.getLogger(GameLoopService::class.java)
    private val running = AtomicBoolean(false)
    private val executor =
        Executors.newSingleThreadExecutor(ThreadFactory { runnable ->
            Thread(runnable, "GameTickScheduler").apply { isDaemon = true }
        })
    private val dispatcher = executor.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val phaseTimer = TickPhaseTimer()

    private val inboundPhase = InboundPacketPhase(entityProcessor)
    private val worldMaintenancePhase = WorldMaintenancePhase(worldProcessor, farmingProcess, plunderDoor)
    private val npcMainPhase = NpcMainPhase(entityProcessor)
    private val playerMainPhase = PlayerMainPhase(entityProcessor)
    private val legacyActionPhase = LegacyActionPhase(actionProcessor, itemProcessor, shopProcessor, objectProcess)
    private val movementFinalizePhase = MovementFinalizePhase(entityProcessor)
    private val outboundSyncPhase = OutboundSyncPhase(outboundPacketProcessor)

    @Volatile
    private var job: Job? = null
    private var currentCycle = 0L
    private var excessCycleNanos = 0L
    private var debugTick = 0
    private var accumulatedCycleTimeMs = 0L

    fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }
        job =
            scope.launch {
                while (isActive && running.get()) {
                    val elapsedNanos = measureNanoTime { runTick() } + excessCycleNanos
                    val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos)
                    val overdue = elapsedMillis > GAME_TICK_INTERVAL_MS
                    val sleepTime =
                        if (overdue) {
                            val elapsedCycleCount = elapsedMillis / GAME_TICK_INTERVAL_MS
                            ((elapsedCycleCount + 1) * GAME_TICK_INTERVAL_MS) - elapsedMillis
                        } else {
                            GAME_TICK_INTERVAL_MS - elapsedMillis
                        }
                    if (overdue) {
                        logger.warn("Game loop overran tick budget: {}ms", elapsedMillis)
                    }
                    maybeLogCycle(elapsedMillis, sleepTime)
                    excessCycleNanos = elapsedNanos - TimeUnit.MILLISECONDS.toNanos(elapsedMillis)
                    delay(sleepTime)
                }
            }
    }

    fun stop(timeout: Duration) {
        running.set(false)
        runBlocking { job?.cancel() }
        executor.shutdown()
        if (!executor.awaitTermination(timeout.toMillis().coerceAtLeast(1L), TimeUnit.MILLISECONDS)) {
            executor.shutdownNow()
        }
    }

    private fun runTick() {
        currentCycle++
        val now = System.currentTimeMillis()
        phaseTimer.clear()
        timed(GamePhase.INBOUND_PACKETS) { inboundPhase.run() }
        timed(GamePhase.WORLD_MAINTENANCE) { worldMaintenancePhase.run(currentCycle, now) }
        timed(GamePhase.NPC_MAIN) { npcMainPhase.run(now) }
        timed(GamePhase.PLAYER_MAIN) { playerMainPhase.run() }
        timed(GamePhase.LEGACY_ACTIONS) { legacyActionPhase.run() }
        timed(GamePhase.MOVEMENT_FINALIZE) { movementFinalizePhase.run() }
        timed(GamePhase.OUTBOUND_SYNC) { outboundSyncPhase.run() }
        timed(GamePhase.HOUSEKEEPING) { entityProcessor.runHousekeepingPhase(now) }
    }

    private fun timed(phase: GamePhase, block: () -> Unit) {
        if (!runtimePhaseTimingEnabled) {
            block()
            return
        }
        phaseTimer.measure(phase) {
            val elapsed = measureNanoTime(block)
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsed)
            if (elapsedMs >= runtimePhaseWarnMs) {
                logger.warn("Phase {} took {}ms", phase, elapsedMs)
            }
        }
    }

    private fun maybeLogCycle(elapsedMillis: Long, sleepTime: Long) {
        if (!runtimeCycleLogEnabled) {
            return
        }
        val interval = runtimeCycleLogIntervalTicks.coerceAtLeast(1)
        debugTick++
        accumulatedCycleTimeMs += elapsedMillis
        if (debugTick < interval) {
            return
        }
        val average = accumulatedCycleTimeMs.toDouble() / debugTick.toDouble()
        logger.info(
            "[Cycle time: {}ms avg / {}ms last] [Sleep: {}ms] [Players: {}] [Tick: {}]",
            String.format("%.2f", average),
            elapsedMillis,
            sleepTime,
            PlayerHandler.getPlayerCount(),
            currentCycle,
        )
        debugTick = 0
        accumulatedCycleTimeMs = 0L
    }

    private companion object {
        private const val GAME_TICK_INTERVAL_MS = 600L
    }
}
