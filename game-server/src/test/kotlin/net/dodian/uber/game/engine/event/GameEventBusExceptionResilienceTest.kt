package net.dodian.uber.game.engine.event

import net.dodian.uber.game.events.GameEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameEventBusExceptionResilienceTest {
    data class TestEvent(val value: Int) : GameEvent

    @AfterEach
    fun tearDown() {
        GameEventBus.clear()
    }

    @Test
    fun `post survives listener runtime exceptions`() {
        GameEventBus.on<TestEvent> { throw IllegalStateException("boom") }

        assertDoesNotThrow { GameEventBus.post(TestEvent(1)) }
        assertFalse(GameEventBus.postWithResult(TestEvent(1)))
    }

    @Test
    fun `postAndReturn isolates returnable listener runtime exceptions`() {
        GameEventBus.onReturnable<TestEvent, String> { throw IllegalStateException("boom") }

        assertDoesNotThrow {
            GameEventBus.postAndReturn<TestEvent, String>(TestEvent(1))
        }
        val results = GameEventBus.postAndReturn<TestEvent, String>(TestEvent(1))
        assertTrue(results.isEmpty())
    }
}
