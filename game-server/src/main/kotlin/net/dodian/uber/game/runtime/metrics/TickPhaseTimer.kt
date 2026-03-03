package net.dodian.uber.game.runtime.metrics

import java.util.EnumMap
import net.dodian.uber.game.runtime.loop.GamePhase

class TickPhaseTimer {
    private val totals = EnumMap<GamePhase, Long>(GamePhase::class.java)

    fun <T> measure(phase: GamePhase, block: () -> T): T {
        val started = System.nanoTime()
        try {
            return block()
        } finally {
            totals[phase] = (totals[phase] ?: 0L) + (System.nanoTime() - started)
        }
    }

    fun clear() {
        totals.clear()
    }
}
