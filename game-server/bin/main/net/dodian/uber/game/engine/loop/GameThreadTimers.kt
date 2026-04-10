package net.dodian.uber.game.engine.loop

import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime
import org.slf4j.LoggerFactory

object GameThreadTimers {
    private val logger = LoggerFactory.getLogger(GameThreadTimers::class.java)
    private val sequence = AtomicLong(1L)
    private val queue = PriorityQueue<ScheduledTask>(compareBy<ScheduledTask> { it.dueAtMillis }.thenBy { it.id })

    @JvmStatic
    fun schedule(label: String, delayMs: Long, context: String = "", task: Runnable): Long {
        val scheduled =
            ScheduledTask(
                id = sequence.getAndIncrement(),
                label = label,
                context = context,
                dueAtMillis = System.currentTimeMillis() + delayMs.coerceAtLeast(0L),
                runnable = task,
            )
        synchronized(queue) {
            queue.add(scheduled)
        }
        return scheduled.id
    }

    @JvmStatic
    fun drainDue(maxTasks: Int = DEFAULT_DRAIN_MAX): DrainStats {
        var processed = 0
        var slowestTaskLabel = ""
        var slowestTaskMs = 0L
        while (processed < maxTasks) {
            val task = synchronized(queue) {
                val next = queue.peek() ?: return@synchronized null
                if (next.dueAtMillis > System.currentTimeMillis()) {
                    null
                } else {
                    queue.poll()
                }
            } ?: break
            try {
                val elapsedMs =
                    measureNanoTime {
                        task.runnable.run()
                    } / 1_000_000L
                if (elapsedMs > slowestTaskMs) {
                    slowestTaskMs = elapsedMs
                    slowestTaskLabel = task.label
                }
            } catch (exception: Throwable) {
                logger.warn(
                    "GameThreadTimers task failed label={} context={}",
                    task.label,
                    task.context.ifBlank { "-" },
                    exception,
                )
            }
            processed++
        }
        val remaining = synchronized(queue) { queue.size }
        return DrainStats(processed = processed, remaining = remaining, slowestTaskLabel = slowestTaskLabel, slowestTaskMs = slowestTaskMs)
    }

    @JvmStatic
    fun clearForTests() {
        synchronized(queue) {
            queue.clear()
        }
    }

    data class DrainStats(
        val processed: Int,
        val remaining: Int,
        val slowestTaskLabel: String,
        val slowestTaskMs: Long,
    )

    private data class ScheduledTask(
        val id: Long,
        val label: String,
        val context: String,
        val dueAtMillis: Long,
        val runnable: Runnable,
    )

    private const val DEFAULT_DRAIN_MAX = 512
}
