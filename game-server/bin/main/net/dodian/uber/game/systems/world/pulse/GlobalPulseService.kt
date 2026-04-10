package net.dodian.uber.game.systems.world.pulse

object GlobalPulseService {
    const val FIVE_MINUTE_PULSE_MS: Long = 300_000L
    const val FIVE_MINUTE_PULSE_TICKS: Long = 500L

    @JvmStatic
    fun isDue(cycle: Long, pulseTicks: Long = FIVE_MINUTE_PULSE_TICKS): Boolean {
        if (pulseTicks <= 0L) {
            return false
        }
        return cycle % pulseTicks == 0L
    }
}

