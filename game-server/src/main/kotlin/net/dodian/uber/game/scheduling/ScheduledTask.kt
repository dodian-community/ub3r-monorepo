package net.dodian.uber.game.scheduling

abstract class ScheduledTask(
    var delay: Int,
    immediate: Boolean = true,
) {
    private var pulses: Int = if (immediate) 0 else delay
    private var running: Boolean = true

    val isRunning get() = running

    abstract fun execute()

    open fun stop() {
        running = false
    }

    fun pulse() {
        if (!running || --pulses <= 0)
            return

        execute()
        pulses = delay
    }
}