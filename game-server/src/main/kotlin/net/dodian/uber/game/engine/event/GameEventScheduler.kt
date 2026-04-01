package net.dodian.uber.game.engine.event

import java.util.function.BooleanSupplier
import kotlin.math.max
import net.dodian.uber.game.engine.scheduler.QueueTask
import net.dodian.uber.game.engine.scheduler.QueueTaskHandle
import net.dodian.uber.game.engine.scheduler.QueueTaskService
import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import net.dodian.uber.game.engine.tasking.TaskHandle

object GameEventScheduler {
    private const val GAME_TICK_MS = 600

    @JvmStatic
    fun schedule(
        delayTicks: Int,
        intervalTicks: Int = 0,
        task: QueueTask,
    ): QueueTaskHandle = QueueTaskService.schedule(delayTicks, intervalTicks, task)

    @JvmStatic
    fun schedule(
        delayTicks: Int,
        intervalTicks: Int = 0,
        action: BooleanSupplier,
    ): QueueTaskHandle = QueueTaskService.schedule(delayTicks, intervalTicks, QueueTask { action.asBoolean })

    @JvmStatic
    fun runLater(delayTicks: Int, action: Runnable): TaskHandle {
        return GameTaskRuntime.queueWorld {
            wait(delayTicks.coerceAtLeast(0))
            action.run()
        }
    }

    @JvmStatic
    fun runLaterMs(delayMs: Int, action: Runnable): TaskHandle {
        return runLater(delayMsToTicks(delayMs), action)
    }

    @JvmStatic
    fun runRepeating(
        delayTicks: Int,
        intervalTicks: Int = 1,
        action: BooleanSupplier,
    ): TaskHandle {
        return GameTaskRuntime.queueWorld {
            val initialDelay = delayTicks.coerceAtLeast(0)
            if (initialDelay > 0) {
                wait(initialDelay)
            }
            val repeating = intervalTicks.coerceAtLeast(1)
            while (action.asBoolean) {
                wait(repeating)
            }
        }
    }

    @JvmStatic
    fun runRepeatingMs(
        delayMs: Int,
        action: BooleanSupplier,
    ): TaskHandle {
        return runRepeatingMs(delayMs, delayMs, action)
    }

    @JvmStatic
    fun runRepeatingMs(
        delayMs: Int,
        intervalMs: Int = delayMs,
        action: BooleanSupplier,
    ): TaskHandle {
        return runRepeating(delayMsToTicks(delayMs), delayMsToTicks(intervalMs), action)
    }

    @JvmStatic
    fun delayMsToTicks(delayMs: Int): Int = max(1, (max(0, delayMs) / GAME_TICK_MS) + 1)
}
