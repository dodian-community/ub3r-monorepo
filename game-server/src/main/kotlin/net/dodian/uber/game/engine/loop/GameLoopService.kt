package net.dodian.uber.game.engine.loop

import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureNanoTime
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.processing.ActionProcessor
import net.dodian.uber.game.engine.processing.EntityProcessor
import net.dodian.uber.game.engine.processing.ItemProcessor
import net.dodian.uber.game.engine.processing.PlunderDoorProcessor
import net.dodian.uber.game.engine.processing.ShopProcessor
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.engine.systems.combat.CombatHitQueueService
import net.dodian.uber.game.engine.phases.InboundPacketPhase
import net.dodian.uber.game.engine.phases.MovementFinalizePhase
import net.dodian.uber.game.engine.phases.NpcMainPhase
import net.dodian.uber.game.engine.phases.OutboundPacketProcessor
import net.dodian.uber.game.engine.phases.PlayerMainPhase
import net.dodian.uber.game.engine.phases.WorldMaintenancePhase
import net.dodian.uber.game.engine.metrics.OperationalTelemetry
import org.slf4j.LoggerFactory

class GameLoopService(
    private val entityProcessor: EntityProcessor = EntityProcessor(),
    private val actionProcessor: ActionProcessor = ActionProcessor(),
    private val outboundPacketProcessor: OutboundPacketProcessor = OutboundPacketProcessor(),
    private val itemProcessor: ItemProcessor = ItemProcessor(),
    private val shopProcessor: ShopProcessor = ShopProcessor(),
    private val plunderDoor: PlunderDoorProcessor = PlunderDoorProcessor(),
) {
    private val logger = LoggerFactory.getLogger(GameLoopService::class.java)
    private val running = AtomicBoolean(false)
    private val boundGameThread = AtomicBoolean(false)
    private val executor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(ThreadFactory { runnable ->
            Thread(runnable, "GameTickScheduler").apply { isDaemon = true }
        })

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
            OperationalTelemetry.recordTick(elapsedMillis, GAME_TICK_INTERVAL_MS)
            if (elapsedMillis > GAME_TICK_INTERVAL_MS) {
                logger.warn("Game loop overran tick budget: {}ms", elapsedMillis)
            }
            maybeLogCycle(elapsedMillis)
        } catch (exception: Throwable) {
            logLoopFailure("tick", exception)
        }
        triggerShutdownIfRequested()
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
        timedPhase("timers.pre") { GameThreadTimers.drainDue() }
        timedPhase("ingress.tick") { GameThreadIngress.drainTickIngress() }
        timedPhase("inbound") { inboundPhase.run() }
        timedPhase("worldDb.input") { worldMaintenancePhase.runWorldDbInputBuild(currentCycle) }
        timedPhase("worldDb.read") { worldMaintenancePhase.runWorldDbResultRead(currentCycle) }
        timedPhase("worldDb.apply") { worldMaintenancePhase.runWorldDbApply(currentCycle) }
        timedPhase("plunder") { worldMaintenancePhase.runPlunder(now) }
        timedPhase("npc.main") { npcMainPhase.run(now) }
        timedPhase("player.main") { playerMainPhase.run() }
        timedPhase("world.tasks") { worldMaintenancePhase.runWorldTasks() }
        timedPhase("combat.hitQueue") { CombatHitQueueService.process(currentCycle) }
        timedPhase("groundItems") { worldMaintenancePhase.runGroundItems() }
        timedPhase("shops") { worldMaintenancePhase.runShops() }
        timedPhase("movement.finalize") { movementFinalizePhase.run() }
        timedPhase("outbound") { outboundPacketProcessor.run() }
        timedPhase("housekeeping") { entityProcessor.runHousekeepingPhase(now) }
        timedPhase("timers.post") { GameThreadTimers.drainDue() }
    }

    private inline fun timedPhase(
        name: String,
        block: () -> Unit,
    ) {
        val elapsedNs = measureNanoTime(block)
        OperationalTelemetry.recordPhaseMillis(name, TimeUnit.NANOSECONDS.toMillis(elapsedNs))
    }

    private fun maybeLogCycle(elapsedMillis: Long) {
        debugTick++
        accumulatedCycleTimeMs += elapsedMillis
        accumulatedLoggedMillis += GAME_TICK_INTERVAL_MS
        if (accumulatedLoggedMillis < CYCLE_LOG_INTERVAL_MS) {
            return
        }
        val average = accumulatedCycleTimeMs.toDouble() / debugTick.toDouble()
        val runtime = Runtime.getRuntime()
        val usedMemoryMb = ((runtime.totalMemory() - runtime.freeMemory()) / MB).toInt()
        val maxMemoryMb = (runtime.maxMemory() / MB).toInt()
        logger.info(
            "[Cycle time: {}ms avg / {}ms last] [Players: {}] [Tick: {}] [mem={}/{}MB]",
            String.format("%.2f", average),
            elapsedMillis,
            PlayerRegistry.getPlayerCount(),
            currentCycle,
            usedMemoryMb,
            maxMemoryMb,
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

    companion object {
        private val staticLogger = LoggerFactory.getLogger(GameLoopService::class.java)
        private const val MB = 1024L * 1024L
        private const val GAME_TICK_INTERVAL_MS = 600L
        private const val CYCLE_LOG_INTERVAL_MS = 60_000L
        private const val IDLE_INGRESS_INTERVAL_MS = 25L
        private const val IDLE_INGRESS_CRITICAL_MAX = 64
        private val shutdownRequested = AtomicBoolean(false)
        private val shutdownSignalEmitted = AtomicBoolean(false)

        @JvmStatic
        fun requestShutdown(reason: String) {
            if (!shutdownRequested.compareAndSet(false, true)) {
                return
            }
            staticLogger.info("Game loop shutdown requested: {}", reason)
        }
    }

    private fun triggerShutdownIfRequested() {
        if (!shutdownRequested.get() || !shutdownSignalEmitted.compareAndSet(false, true)) {
            return
        }
        Thread(
            {
                try {
                    Server.shutdown()
                } catch (exception: RuntimeException) {
                    logger.error("Controlled shutdown failed", exception)
                }
            },
            "GameShutdown-Dispatcher",
        ).apply {
            isDaemon = true
            start()
        }
    }
}
