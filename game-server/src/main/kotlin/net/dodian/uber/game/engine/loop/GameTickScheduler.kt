package net.dodian.uber.game.engine.loop

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

/**
 * Single-threaded scheduler driven by the game tick.
 */
class GameTickScheduler(
    private val tickIntervalMs: Long,
) {
    private val lock = Any()
    private val tasks = mutableListOf<ScheduledTask>()

    @Volatile
    private var executor: ScheduledExecutorService? = null
    private var started = false
    private var nextPlannedTickTimeMs = -1L

    init {
        require(tickIntervalMs > 0) { "tickIntervalMs must be > 0" }
    }

    fun registerTask(name: String, intervalMs: Long, runnable: Runnable) {
        require(intervalMs > 0) { "intervalMs must be > 0" }
        synchronized(lock) {
            val task = ScheduledTask(name, intervalMs, runnable)
            if (started) {
                task.initializeNextRun(System.currentTimeMillis())
            }
            tasks.add(task)
        }
    }

    fun start() {
        synchronized(lock) {
            if (started) {
                return
            }

            val now = System.currentTimeMillis()
            tasks.forEach { it.initializeNextRun(now) }
            nextPlannedTickTimeMs = now

            val threadFactory = ThreadFactory { runnable ->
                Thread(runnable, "GameTickScheduler").apply { isDaemon = true }
            }
            executor = Executors.newSingleThreadScheduledExecutor(threadFactory)
            started = true
            executor?.scheduleAtFixedRate(::safeTick, 0, tickIntervalMs, TimeUnit.MILLISECONDS)
        }
    }

    fun stop() {
        val toStop: ScheduledExecutorService?
        synchronized(lock) {
            if (!started && executor == null) {
                return
            }
            started = false
            toStop = executor
            executor = null
            nextPlannedTickTimeMs = -1L
            tasks.forEach { it.reset() }
        }

        if (toStop != null) {
            toStop.shutdown()
            try {
                if (!toStop.awaitTermination(5, TimeUnit.SECONDS)) {
                    toStop.shutdownNow()
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                toStop.shutdownNow()
            }
        }
    }

    private fun safeTick() {
        try {
            runTickForTesting(System.currentTimeMillis())
        } catch (t: Throwable) {
            logger.error("Game tick scheduler encountered an unexpected error.", t)
        }
    }

    internal fun runTickForTesting(nowMs: Long) {
        synchronized(lock) {
            if (nextPlannedTickTimeMs == -1L) {
                tasks.forEach { it.initializeNextRun(nowMs) }
                nextPlannedTickTimeMs = nowMs
            }

            val plannedTickTime = nextPlannedTickTimeMs
            nextPlannedTickTimeMs += tickIntervalMs

            if (DEBUG_SCHEDULER_LAG) {
                val lag = nowMs - plannedTickTime
                if (lag > 0) {
                    logger.debug("Game tick lag={}ms", lag)
                }
            }

            for (task in tasks) {
                if (!task.isDue(nowMs)) {
                    continue
                }
                try {
                    task.runnable.run()
                } catch (t: Throwable) {
                    logger.error("Scheduled task '{}' failed.", task.name, t)
                }
                task.advanceNextRun(nowMs)
            }
        }
    }

    private class ScheduledTask(
        val name: String,
        private val intervalMs: Long,
        val runnable: Runnable,
    ) {
        private var nextRunMs = -1L

        fun initializeNextRun(nowMs: Long) {
            if (nextRunMs == -1L) {
                nextRunMs = nowMs
            }
        }

        fun isDue(nowMs: Long): Boolean = nextRunMs != -1L && nowMs >= nextRunMs

        fun advanceNextRun(nowMs: Long) {
            do {
                nextRunMs += intervalMs
            } while (nextRunMs <= nowMs)
        }

        fun reset() {
            nextRunMs = -1L
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(GameTickScheduler::class.java)
        private const val DEBUG_SCHEDULER_LAG = false
    }
}
