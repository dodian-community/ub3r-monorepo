package net.dodian.uber.game.tasks

import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TickTasksFacadeTest {
    @Test
    fun `worldTaskCoroutine supports tick delay semantics`() {
        GameTaskRuntime.clear()
        val events = mutableListOf<String>()

        TickTasks.worldTaskCoroutine {
            events += "start"
            delay(2)
            events += "after-2"
        }

        repeat(4) { GameTaskRuntime.cycleWorld() }
        assertEquals(listOf("start", "after-2"), events)
    }

    @Test
    fun `worldTaskCoroutine handle cancellation prevents execution`() {
        GameTaskRuntime.clear()
        var invoked = false

        val handle =
            TickTasks.worldTaskCoroutine {
                delay(4)
                invoked = true
            }
        handle.cancel()

        repeat(6) { GameTaskRuntime.cycleWorld() }
        assertTrue(handle.isCancelled())
        assertFalse(invoked)
    }
}
