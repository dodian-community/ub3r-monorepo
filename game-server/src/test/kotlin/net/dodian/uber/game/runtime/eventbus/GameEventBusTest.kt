package net.dodian.uber.game.event

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameEventBusTest {

    data class TestEvent(val value: Int) : GameEvent
    data class ReturnEvent(val name: String) : GameEvent

    @AfterEach
    fun tearDown() {
        GameEventBus.clear()
    }

    @Test
    fun `listeners fire in registration order`() {
        val trace = mutableListOf<String>()
        GameEventBus.on<TestEvent> { trace += "first"; false }
        GameEventBus.on<TestEvent> { trace += "second"; false }

        GameEventBus.post(TestEvent(1))

        assertEquals(listOf("first", "second"), trace)
    }

    @Test
    fun `filters block listener execution`() {
        val trace = mutableListOf<String>()
        GameEventBus.addFilter<TestEvent> { it.value > 1 }
        GameEventBus.on<TestEvent> { trace += "ran"; true }

        assertFalse(GameEventBus.postWithResult(TestEvent(1)))
        assertTrue(trace.isEmpty())
    }

    @Test
    fun `post with result reflects handled state`() {
        GameEventBus.on<TestEvent>(condition = { it.value == 2 }) { true }

        assertFalse(GameEventBus.postWithResult(TestEvent(1)))
        assertTrue(GameEventBus.postWithResult(TestEvent(2)))
    }

    @Test
    fun `post and return aggregates results`() {
        GameEventBus.onReturnable<ReturnEvent, String> { "${it.name}-a" }
        GameEventBus.onReturnable<ReturnEvent, String> { "${it.name}-b" }

        val results = GameEventBus.postAndReturn<ReturnEvent, String>(ReturnEvent("probe"))

        assertEquals(listOf("probe-a", "probe-b"), results)
    }
}
