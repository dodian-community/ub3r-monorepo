package net.dodian.uber.game.engine.loop

import java.util.concurrent.atomic.AtomicLong
import net.dodian.uber.game.Server
import net.dodian.uber.game.systems.world.player.PlayerRegistry

object GameCycleClock {
    private val cycle = AtomicLong(0L)

    @JvmStatic
    fun currentCycle(): Long = cycle.get()

    @JvmStatic
    fun advance(): Long {
        val next = cycle.incrementAndGet()
        syncPlayerRegistryCycle(next)
        return next
    }

    @JvmStatic
    fun syncTo(value: Long) {
        cycle.set(value.coerceAtLeast(0L))
        syncPlayerRegistryCycle(cycle.get())
    }

    @JvmStatic
    fun ticksForDurationMs(durationMs: Long): Int {
        if (durationMs <= 0L) {
            return 1
        }
        val tickMs = Server.TICK.toLong().coerceAtLeast(1L)
        return ((durationMs + tickMs - 1L) / tickMs).toInt().coerceAtLeast(1)
    }

    private fun syncPlayerRegistryCycle(value: Long) {
        val tick = value.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        PlayerRegistry.cycle = tick
    }
}
