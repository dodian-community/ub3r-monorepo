package net.dodian.uber.game.runtime.queue

import java.util.PriorityQueue
import net.dodian.uber.game.model.entity.player.PlayerHandler

object QueueTaskService {
    private val scheduled =
        PriorityQueue<ScheduledTask>(compareBy<ScheduledTask> { it.dueCycle }.thenBy { it.id })
    private var nextId = 0L

    @JvmStatic
    fun schedule(
        delayTicks: Int,
        intervalTicks: Int = 0,
        task: QueueTask,
    ): QueueTaskHandle {
        val token = QueueCancellationToken()
        val dueCycle = PlayerHandler.cycle.toLong() + delayTicks.coerceAtLeast(0)
        scheduled += ScheduledTask(++nextId, dueCycle, intervalTicks.coerceAtLeast(0), token, task)
        return QueueTaskHandle(token)
    }

    @JvmStatic
    fun processDue(currentCycle: Long = PlayerHandler.cycle.toLong()) {
        while (scheduled.isNotEmpty() && scheduled.peek().dueCycle <= currentCycle) {
            val next = scheduled.poll()
            if (next.token.cancelled) {
                continue
            }
            val keepRunning = next.task.execute()
            if (keepRunning && !next.token.cancelled && next.intervalTicks > 0) {
                scheduled += next.copy(dueCycle = currentCycle + next.intervalTicks)
            }
        }
    }

    @JvmStatic
    fun processDue() {
        processDue(PlayerHandler.cycle.toLong())
    }

    private data class ScheduledTask(
        val id: Long,
        val dueCycle: Long,
        val intervalTicks: Int,
        val token: QueueCancellationToken,
        val task: QueueTask,
    )
}
