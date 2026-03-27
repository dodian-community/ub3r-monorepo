package net.dodian.uber.game.engine.loop

import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureNanoTime
import net.dodian.jobs.impl.ActionProcessor
import net.dodian.jobs.impl.EntityProcessor
import net.dodian.jobs.impl.ItemProcessor
import net.dodian.jobs.impl.PlunderDoor
import net.dodian.jobs.impl.ShopProcessor
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.systems.combat.CombatHitQueueService
import net.dodian.uber.game.engine.metrics.TickPhaseTimer
import net.dodian.uber.game.engine.metrics.GcStallTracker
import net.dodian.uber.game.engine.phases.InboundPacketPhase
import net.dodian.uber.game.engine.phases.MovementFinalizePhase
import net.dodian.uber.game.engine.phases.NpcMainPhase
import net.dodian.uber.game.engine.phases.OutboundPacketProcessor
import net.dodian.uber.game.engine.phases.PlayerMainPhase
import net.dodian.uber.game.engine.phases.WorldMaintenancePhase
import org.slf4j.LoggerFactory
import net.dodian.utilities.runtimeCycleLogEnabled
import net.dodian.utilities.runtimePhaseTimingEnabled
import net.dodian.utilities.runtimePhaseWarnMs

class GameLoopService(
    private val entityProcessor: EntityProcessor = EntityProcessor(),
    private val actionProcessor: ActionProcessor = ActionProcessor(),
    private val outboundPacketProcessor: OutboundPacketProcessor = OutboundPacketProcessor(),
    private val itemProcessor: ItemProcessor = ItemProcessor(),
    private val shopProcessor: ShopProcessor = ShopProcessor(),
    private val plunderDoor: PlunderDoor = PlunderDoor(),
) {
    private val logger = LoggerFactory.getLogger(GameLoopService::class.java)
    private val running = AtomicBoolean(false)
    private val boundGameThread = AtomicBoolean(false)
    private val executor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(ThreadFactory { runnable ->
            Thread(runnable, "GameTickScheduler").apply { isDaemon = true }
        })
    private val phaseTimer = TickPhaseTimer()
    private val gcTracker = GcStallTracker()

    private val inboundPhase = InboundPacketPhase(entityProcessor)
    private val worldMaintenancePhase = WorldMaintenancePhase(plunderDoor, actionProcessor, itemProcessor, shopProcessor)
    private val npcMainPhase = NpcMainPhase(entityProcessor)
    private val playerMainPhase = PlayerMainPhase(entityProcessor)
    private val movementFinalizePhase = MovementFinalizePhase(entityProcessor)
    @Volatile
    private var tickFuture: ScheduledFuture<*>? = null
    @Volatile
    private var ingressFuture: ScheduledFuture<*>? = null
    private var currentCycle = 0L
    private var debugTick = 0
    private var accumulatedCycleTimeMs = 0L
    private var accumulatedLoggedMillis = 0L
    @Volatile
    private var lastLoopFailureSignature = ""
    @Volatile
    private var lastLoopFailureCount = 0

    fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }
        tickFuture = executor.scheduleAtFixedRate(::runScheduledTick, 0L, GAME_TICK_INTERVAL_MS, TimeUnit.MILLISECONDS)
        ingressFuture =
            executor.scheduleAtFixedRate(
                ::runIdleIngressScheduled,
                IDLE_INGRESS_INTERVAL_MS,
                IDLE_INGRESS_INTERVAL_MS,
                TimeUnit.MILLISECONDS,
            )
    }

    fun stop(timeout: Duration) {
        running.set(false)
        tickFuture?.cancel(false)
        ingressFuture?.cancel(false)
        executor.shutdown()
        if (!executor.awaitTermination(timeout.toMillis().coerceAtLeast(1L), TimeUnit.MILLISECONDS)) {
            executor.shutdownNow()
        }
    }

    private fun runScheduledTick() {
        bindGameThread()
        if (!running.get()) {
            return
        }
        try {
            val elapsedNanos = measureNanoTime { runTick() }
            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos)
            if (elapsedMillis > GAME_TICK_INTERVAL_MS) {
                logger.warn("Game loop overran tick budget: {}ms", elapsedMillis)
            }
            maybeLogCycle(elapsedMillis)
        } catch (exception: Throwable) {
            logLoopFailure("tick", exception)
        }
    }

    private fun runIdleIngressScheduled() {
        bindGameThread()
        if (!running.get()) {
            return
        }
        try {
            runIdleIngress()
        } catch (exception: Throwable) {
            logLoopFailure("idle-ingress", exception)
        }
    }

    private fun runIdleIngress() {
        GameThreadIngress.drainCritical(IDLE_INGRESS_CRITICAL_MAX)
        GameThreadTimers.drainDue()
    }

    private fun bindGameThread() {
        if (boundGameThread.compareAndSet(false, true)) {
            GameThreadContext.bindCurrentThread()
        }
    }

    private fun runTick() {
        currentCycle = GameCycleClock.advance()
        val now = System.currentTimeMillis()
        phaseTimer.clear()
        GameThreadTimers.drainDue()
        timed(GamePhase.LOGIN_INGRESS) { GameThreadIngress.drainTickIngress() }
        timed(GamePhase.INBOUND_PACKETS) { inboundPhase.run() }
        timed(GamePhase.WORLD_DB_INPUT_BUILD) { worldMaintenancePhase.runWorldDbInputBuild(currentCycle) }
        timed(GamePhase.WORLD_DB_RESULT_READ) { worldMaintenancePhase.runWorldDbResultRead(currentCycle) }
        timed(GamePhase.WORLD_DB_APPLY) { worldMaintenancePhase.runWorldDbApply(currentCycle) }
        timed(GamePhase.FARMING_TICK) { worldMaintenancePhase.runFarming(currentCycle) }
        timed(GamePhase.PLUNDER_DOOR) { worldMaintenancePhase.runPlunder(now) }
        timed(GamePhase.NPC_MAIN) { npcMainPhase.run(now) }
        timed(GamePhase.PLAYER_MAIN) { playerMainPhase.run() }
        timed(GamePhase.WORLD_TASKS) {
            worldMaintenancePhase.runWorldTasks()
            CombatHitQueueService.process(currentCycle)
        }
        timed(GamePhase.GROUND_ITEMS) { worldMaintenancePhase.runGroundItems() }
        timed(GamePhase.SHOPS) { worldMaintenancePhase.runShops() }
        timed(GamePhase.MOVEMENT_FINALIZE) { movementFinalizePhase.run() }
        timed(GamePhase.OUTBOUND_SYNC) { outboundPacketProcessor.run() }
        timed(GamePhase.HOUSEKEEPING) { entityProcessor.runHousekeepingPhase(now) }
        GameThreadTimers.drainDue()
    }

    private fun timed(phase: GamePhase, block: () -> Unit) {
        if (!runtimePhaseTimingEnabled) {
            block()
            return
        }
        phaseTimer.measure(phase) {
            val gcBefore = gcTracker.snapshot()
            val elapsed = measureNanoTime(block)
            val gcAfter = gcTracker.snapshot()
            val gcDelta = gcTracker.delta(gcBefore, gcAfter)
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsed)
            if (elapsedMs >= runtimePhaseWarnMs) {
                if (gcDelta.collectionTimeMs > 0L || gcDelta.collectionCount > 0L) {
                    logger.warn(
                        "Phase {} took {}ms (gc={}ms/{} collections)",
                        phase,
                        elapsedMs,
                        gcDelta.collectionTimeMs,
                        gcDelta.collectionCount,
                    )
                } else {
                    logger.warn("Phase {} took {}ms", phase, elapsedMs)
                }
            }
        }
    }

    private fun maybeLogCycle(elapsedMillis: Long) {
        if (!runtimeCycleLogEnabled) {
            return
        }
        debugTick++
        accumulatedCycleTimeMs += elapsedMillis
        accumulatedLoggedMillis += GAME_TICK_INTERVAL_MS
        if (accumulatedLoggedMillis < CYCLE_LOG_INTERVAL_MS) {
            return
        }
        val average = accumulatedCycleTimeMs.toDouble() / debugTick.toDouble()
        logger.info(
            "[Cycle time: {}ms avg / {}ms last] [Players: {}] [Tick: {}]",
            String.format("%.2f", average),
            elapsedMillis,
            PlayerHandler.getPlayerCount(),
            currentCycle,
        )
        debugTick = 0
        accumulatedCycleTimeMs = 0L
        accumulatedLoggedMillis = 0L
    }

    private fun logLoopFailure(scope: String, exception: Throwable) {
        val signature = "$scope:${exception::class.java.name}:${exception.message ?: ""}"
        if (signature == lastLoopFailureSignature) {
            lastLoopFailureCount++
            if (lastLoopFailureCount <= 3 || lastLoopFailureCount % 50 == 0) {
                logger.error(
                    "Game loop {} failure repeated {} times; latest error: {}",
                    scope,
                    lastLoopFailureCount,
                    exception.message ?: exception::class.java.simpleName,
                )
            }
            return
        }
        lastLoopFailureSignature = signature
        lastLoopFailureCount = 1
        logger.error("Game loop {} encountered an unexpected error but will keep running.", scope, exception)
    }

    private companion object {
        private const val GAME_TICK_INTERVAL_MS = 600L
        private const val CYCLE_LOG_INTERVAL_MS = 60_000L
        private const val IDLE_INGRESS_INTERVAL_MS = 25L
        private const val IDLE_INGRESS_CRITICAL_MAX = 64
    }
}
