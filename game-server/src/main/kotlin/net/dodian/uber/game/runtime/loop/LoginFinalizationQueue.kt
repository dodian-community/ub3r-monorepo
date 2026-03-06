package net.dodian.uber.game.runtime.loop

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.system.measureNanoTime
import org.slf4j.LoggerFactory

object LoginFinalizationQueue {
    private val logger = LoggerFactory.getLogger(LoginFinalizationQueue::class.java)
    private val queue = ConcurrentLinkedQueue<QueuedTask>()

    @JvmStatic
    fun submit(label: String, task: Runnable) {
        queue.add(QueuedTask(label, System.nanoTime(), task))
    }

    @JvmStatic
    fun drain() {
        drain(2_000)
    }

    @JvmStatic
    fun drain(maxTasks: Int) {
        val queueSizeBefore = queue.size
        var processed = 0
        var maxQueueWaitMs = 0L
        var slowestTaskLabel = ""
        var slowestTaskMs = 0L
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
                logger.warn("Login finalization task failed label={}", task.label, exception)
            }
            processed++
        }
        val queueSizeAfter = queue.size
        if (processed >= maxTasks && queueSizeAfter > 0) {
            logger.warn(
                "LoginFinalizationQueue reached maxTasks={} before={} processed={} remaining={} maxQueueWait={}ms slowestTask={}({}ms)",
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

    private data class QueuedTask(
        val label: String,
        val enqueuedAtNanos: Long,
        val runnable: Runnable,
    )
}
