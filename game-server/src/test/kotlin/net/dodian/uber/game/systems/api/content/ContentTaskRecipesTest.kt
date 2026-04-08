package net.dodian.uber.game.systems.api.content

import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentTaskRecipesTest {
    @Test
    fun `world countdown emits ticks in descending order and completes once`() {
        GameTaskRuntime.clear()
        val ticks = mutableListOf<Int>()
        var completed = 0

        val handle =
            ContentTaskRecipes.worldCountdown(totalTicks = 3, onTick = { remaining ->
                ticks += remaining
            }) {
                completed++
            }

        repeat(6) { GameTaskRuntime.cycleWorld() }

        assertEquals(listOf(3, 2, 1), ticks)
        assertEquals(1, completed)
        assertTrue(handle.isCompleted())
    }
}
