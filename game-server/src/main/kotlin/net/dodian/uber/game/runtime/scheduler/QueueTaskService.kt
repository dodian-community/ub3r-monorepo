package net.dodian.uber.game.runtime.scheduler

import net.dodian.uber.game.runtime.tasking.GameTaskRuntime

object QueueTaskService {
    @JvmStatic
    fun schedule(
        delayTicks: Int,
        intervalTicks: Int = 0,
        task: QueueTask,
    ): QueueTaskHandle {
        val handle =
            GameTaskRuntime.queueWorld {
                val initialDelay = delayTicks.coerceAtLeast(0)
                if (initialDelay > 0) {
                    wait(initialDelay)
                }
                val repeating = intervalTicks.coerceAtLeast(0)
                while (true) {
                    val keepRunning = task.execute()
                    if (!keepRunning || repeating <= 0) {
                        return@queueWorld
                    }
                    wait(repeating)
                }
            }
        return QueueTaskHandle.from(handle)
    }

    @JvmStatic
    fun processDue() {
        GameTaskRuntime.cycle()
    }
}
