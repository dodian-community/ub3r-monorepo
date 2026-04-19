package net.dodian.uber.game.engine.loop

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.system.measureNanoTime
import org.slf4j.LoggerFactory

object GameThreadIngress {
    private val logger = LoggerFactory.getLogger(GameThreadIngress::class.java)
    private val criticalQueue = ConcurrentLinkedQueue<QueuedTask>()
    private val deferredQueue = ConcurrentLinkedQueue<QueuedTask>()

    @JvmStatic
    fun submitCritical(label: String, task: Runnable) {
        criticalQueue.add(QueuedTask(label, System.nanoTime(), task))
    }

    @JvmStatic
    fun submitDeferred(task: Runnable) {
        submitDeferred("anonymous", task)
    }

    @JvmStatic
    fun submitDeferred(label: String, task: Runnable) {
        deferredQueue.add(QueuedTask(label, System.nanoTime(), task))
    }

    @JvmStatic
    fun drainCritical(maxTasks: Int = DEFAULT_CRITICAL_DRAIN_MAX): DrainStats =
        drainQueue(
            queueName = "GameThreadIngress[critical]",
            queue = criticalQueue,
            maxTasks = maxTasks,
            includeLabels = false,
        )

    @JvmStatic
    fun drainDeferred(maxTasks: Int = DEFAULT_DEFERRED_DRAIN_MAX): DrainStats =
        drainQueue(
            queueName = "GameThreadIngress[deferred]",
            queue = deferredQueue,
            maxTasks = maxTasks,
            includeLabels = true,
        )

    @JvmStatic
    fun drainTickIngress(
        maxCriticalTasks: Int = DEFAULT_CRITICAL_DRAIN_MAX,
        maxDeferredTasks: Int = DEFAULT_DEFERRED_DRAIN_MAX,
    ): TickDrainStats {
        val critical = drainCritical(maxCriticalTasks)
        val deferred = drainDeferred(maxDeferredTasks)
        return TickDrainStats(critical = critical, deferred = deferred)
    }

    @JvmStatic
    fun clearForTests() {
        criticalQueue.clear()
        deferredQueue.clear()
    }

    private fun drainQueue(
        queueName: String,
        queue: ConcurrentLinkedQueue<QueuedTask>,
        maxTasks: Int,
        includeLabels: Boolean,
    ): DrainStats {
        val queueSizeBefore = queue.size
        var processed = 0
        var maxQueueWaitMs = 0L
        var slowestTaskLabel = ""
        var slowestTaskMs = 0L
        val processedByLabel = HashMap<String, Int>()
        while (processed < maxTasks) {
            val task = queue.poll() ?: break
            val queueWaitMs = (System.nanoTime() - task.enqueuedAtNanos) / 1_000_000L
            if (queueWaitMs > maxQueueWaitMs) {
                maxQueueWaitMs = queueWaitMs
            }
            try {
                val taskElapsedNs =
                    measureNanoTime {
                        task.runnable.run()
                    }
                val taskElapsedMs = taskElapsedNs / 1_000_000L
                if (taskElapsedMs > slowestTaskMs) {
                    slowestTaskMs = taskElapsedMs
                    slowestTaskLabel = task.label
                }
            } catch (exception: Throwable) {
                logger.warn("{} task failed label={}", queueName, task.label, exception)
            }
            if (includeLabels) {
                processedByLabel[task.label] = (processedByLabel[task.label] ?: 0) + 1
            }
            processed++
        }
        val queueSizeAfter = queue.size
        if (processed >= maxTasks && queueSizeAfter > 0) {
            if (includeLabels) {
                logger.warn(
                    "{} reached maxTasks={} before={} processed={} remaining={} maxQueueWait={}ms slowestTask={}({}ms) labels={}",
                    queueName,
                    maxTasks,
                    queueSizeBefore,
                    processed,
                    queueSizeAfter,
                    maxQueueWaitMs,
                    if (slowestTaskLabel.isEmpty()) "n/a" else slowestTaskLabel,
                    slowestTaskMs,
                    formatTopLabels(processedByLabel),
                )
            } else {
                logger.warn(
                    "{} reached maxTasks={} before={} processed={} remaining={} maxQueueWait={}ms slowestTask={}({}ms)",
                    queueName,
                    maxTasks,
                    queueSizeBefore,
                    processed,
                    queueSizeAfter,
                    maxQueueWaitMs,
                    if (slowestTaskLabel.isEmpty()) "n/a" else slowestTaskLabel,
                    slowestTaskMs,
                )
            }
        }
        return DrainStats(
            processed = processed,
            remaining = queueSizeAfter,
            maxQueueWaitMs = maxQueueWaitMs,
            slowestTaskLabel = slowestTaskLabel,
            slowestTaskMs = slowestTaskMs,
        )
    }

    private fun formatTopLabels(processedByLabel: Map<String, Int>): String {
        if (processedByLabel.isEmpty()) {
            return "[]"
        }
        return processedByLabel.entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString(prefix = "[", postfix = "]") { "${it.key}=${it.value}" }
    }

    data class DrainStats(
        val processed: Int,
        val remaining: Int,
        val maxQueueWaitMs: Long,
        val slowestTaskLabel: String,
        val slowestTaskMs: Long,
    )

    data class TickDrainStats(
        val critical: DrainStats,
        val deferred: DrainStats,
    )

    private data class QueuedTask(
        val label: String,
        val enqueuedAtNanos: Long,
        val runnable: Runnable,
    )

    private const val DEFAULT_CRITICAL_DRAIN_MAX = 2_000
    private const val DEFAULT_DEFERRED_DRAIN_MAX = 10_000
}
