package net.dodian.uber.game.systems.api.content

import net.dodian.uber.game.engine.tasking.TaskHandle

object ContentTaskRecipes {
    @JvmStatic
    @JvmOverloads
    fun worldCountdown(
        totalTicks: Int,
        onTick: (Int) -> Unit = {},
        onDone: () -> Unit,
    ): TaskHandle {
        require(totalTicks > 0) { "totalTicks must be > 0." }
        return ContentScheduling.world {
            var remaining = totalTicks
            repeatEvery(intervalTicks = 1) {
                onTick(remaining)
                remaining--
                if (remaining <= 0) {
                    onDone()
                    return@repeatEvery false
                }
                true
            }
        }
    }
}
