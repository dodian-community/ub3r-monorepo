package net.dodian.uber.game.engine.tasking

import net.dodian.uber.game.engine.metrics.OperationalTelemetry
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskLifecycleTelemetryTest {
    @Test
    fun `task lifecycle emits scheduled active and completed counters`() {
        GameTaskRuntime.clear()
        val beforeScheduled = counter("task.lifecycle.scheduled")
        val beforePending = counter("task.lifecycle.pending")
        val beforeActive = counter("task.lifecycle.active")
        val beforeCompleted = counter("task.lifecycle.completed")

        worldTaskCoroutine { }
        GameTaskRuntime.cycleWorld()

        assertTrue(counter("task.lifecycle.scheduled") > beforeScheduled)
        assertTrue(counter("task.lifecycle.pending") > beforePending)
        assertTrue(counter("task.lifecycle.active") > beforeActive)
        assertTrue(counter("task.lifecycle.completed") > beforeCompleted)
    }

    @Test
    fun `task cancellation emits cancelled counter`() {
        GameTaskRuntime.clear()
        val beforeCancelled = counter("task.lifecycle.cancelled")

        val handle =
            worldTaskCoroutine {
                delay(5)
            }
        handle.cancel("test")
        repeat(2) { GameTaskRuntime.cycleWorld() }

        assertTrue(counter("task.lifecycle.cancelled") > beforeCancelled)
    }

    @Test
    fun `task failure emits failure class counter`() {
        GameTaskRuntime.clear()
        val beforeFailure = counter("task.lifecycle.failure.IllegalStateException")

        worldTaskCoroutine {
            throw IllegalStateException("boom")
        }
        GameTaskRuntime.cycleWorld()

        assertTrue(counter("task.lifecycle.failure.IllegalStateException") > beforeFailure)
    }

    private fun counter(name: String): Long {
        val snapshot = OperationalTelemetry.snapshot()
        @Suppress("UNCHECKED_CAST")
        val counters = snapshot["counters"] as Map<String, Long>
        return counters[name] ?: 0L
    }
}
