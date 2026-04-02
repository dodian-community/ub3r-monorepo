package net.dodian.uber.game.engine.tasking

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import net.dodian.uber.game.engine.tasking.GameTaskRuntime

class TaskCoroutineFacadeTest {
    @Test
    fun `delay zero is immediate and does not suspend`() {
        GameTaskRuntime.clear()
        val events = mutableListOf<String>()

        worldTaskCoroutine {
            events += "start"
            delay(0)
            events += "after-zero"
        }

        GameTaskRuntime.cycleWorld()
        assertEquals(listOf("start", "after-zero"), events)
    }

    @Test
    fun `delay resumes after expected number of cycles`() {
        GameTaskRuntime.clear()
        val events = mutableListOf<String>()

        worldTaskCoroutine {
            events += "start"
            delay(3)
            events += "after-3"
            delay(2)
            events += "after-5"
        }

        repeat(6) { GameTaskRuntime.cycleWorld() }
        assertEquals(listOf("start", "after-3", "after-5"), events)
    }

    @Test
    fun `stop cancels and prevents dead code`() {
        GameTaskRuntime.clear()
        val events = mutableListOf<String>()

        worldTaskCoroutine {
            events += "start"
            stop()
            events += "dead-code"
        }

        repeat(3) { GameTaskRuntime.cycleWorld() }
        assertEquals(listOf("start"), events)
    }

    @Test
    fun `repeatEvery executes expected number of iterations`() {
        GameTaskRuntime.clear()
        val events = mutableListOf<Int>()

        worldTaskCoroutine {
            var tick = 0
            repeatEvery(intervalTicks = 1) {
                events += tick++
                tick < 3
            }
        }

        repeat(6) { GameTaskRuntime.cycleWorld() }
        assertEquals(listOf(0, 1, 2), events)
    }

    @Test
    fun `world task handle cancellation stops execution`() {
        GameTaskRuntime.clear()
        var invoked = false

        val handle =
            worldTaskCoroutine {
                delay(5)
                invoked = true
            }
        handle.cancel()

        repeat(8) { GameTaskRuntime.cycleWorld() }
        assertTrue(handle.isCancelled())
        assertFalse(invoked)
    }
}
