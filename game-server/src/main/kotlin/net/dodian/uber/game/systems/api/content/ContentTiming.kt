package net.dodian.uber.game.systems.api.content

import java.util.function.BooleanSupplier
import net.dodian.uber.game.event.GameEventScheduler
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.engine.loop.GameThreadTimers

object ContentTiming {
    @JvmStatic
    fun currentCycle(): Long = GameCycleClock.currentCycle()

    @JvmStatic
    fun ticksForDurationMs(durationMs: Long): Int = GameCycleClock.ticksForDurationMs(durationMs)

    @JvmStatic
    fun scheduleGameThread(
        label: String,
        delayMs: Long,
        context: String = "",
        task: Runnable,
    ): Long {
        return GameThreadTimers.schedule(label = label, delayMs = delayMs, context = context, task = task)
    }

    @JvmStatic
    fun runLaterMs(
        delayMs: Int,
        action: Runnable,
    ) {
        GameEventScheduler.runLaterMs(delayMs, action)
    }

    @JvmStatic
    fun runRepeatingMs(
        delayMs: Int,
        intervalMs: Int = delayMs,
        action: BooleanSupplier,
    ) {
        GameEventScheduler.runRepeatingMs(delayMs, intervalMs, action)
    }
}
