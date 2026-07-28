package net.dodian.uber.game.engine.tasking

import net.dodian.uber.game.engine.tasking.set.WorldTaskSet
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * GameTask.wait()/waitUntil() used to suspend via plain kotlin.coroutines.suspendCoroutine,
 * and terminate() simply discarded the pending continuation instead of resuming it. That
 * meant try/finally (and use{}) cleanup written inside a suspended task body never ran when
 * the task was forcibly cancelled (logout/death mid-task, priority preemption, owner
 * teardown, etc.) - the coroutine was abandoned rather than unwound.
 *
 * These tests pin down the fixed behavior: terminate() must resume the suspended
 * continuation with a CancellationException so in-flight cleanup actually executes.
 */
class GameTaskCancellationTest {
    @Test
    fun `terminate runs a finally block inside a task suspended on wait`() {
        val set = WorldTaskSet()
        var cleanupRan = false

        val handle =
            set.queue {
                try {
                    wait(5)
                } finally {
                    cleanupRan = true
                }
            }

        set.cycle() // starts the coroutine; it suspends inside wait(5)
        assertFalse(cleanupRan, "cleanup must not run before termination")

        handle.cancel("test-cancel")

        assertTrue(cleanupRan, "finally block should run when a suspended task is terminated")
        assertTrue(handle.isCancelled())
    }

    @Test
    fun `terminate runs a finally block inside a task suspended on waitUntil`() {
        val set = WorldTaskSet()
        var cleanupRan = false

        val handle =
            set.queue {
                try {
                    waitUntil { false }
                } finally {
                    cleanupRan = true
                }
            }

        set.cycle()
        assertFalse(cleanupRan)

        handle.cancel("test-cancel")

        assertTrue(cleanupRan)
    }

    @Test
    fun `terminate on a task with no pending suspension is a no-op cancellation`() {
        val set = WorldTaskSet()
        val handle =
            set.queue {
                // completes synchronously without ever suspending
            }

        set.cycle()

        // Task already completed on its own; cancelling afterwards must not throw
        // or double-run completion bookkeeping.
        handle.cancel("late-cancel")
        assertTrue(handle.isCompleted())
    }
}
