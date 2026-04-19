package net.dodian.uber.game.engine.event

import net.dodian.uber.game.engine.metrics.OperationalTelemetry
import net.dodian.uber.game.events.GameEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameEventBusTelemetryTest {
    data class TelemetryEvent(val value: Int) : GameEvent

    @AfterEach
    fun tearDown() {
        GameEventBus.clear()
    }

    @Test
    fun `post with no subscribers increments missing subscriber counter`() {
        val before = counter("event.dispatch.missing_subscriber.TelemetryEvent")

        GameEventBus.post(TelemetryEvent(1))

        val after = counter("event.dispatch.missing_subscriber.TelemetryEvent")
        assertTrue(after > before)
    }

    @Test
    fun `dispatch exception increments exception counter`() {
        val before = counter("event.dispatch.exception.TelemetryEvent")
        GameEventBus.on<TelemetryEvent> { throw IllegalStateException("boom") }

        GameEventBus.post(TelemetryEvent(1))

        val after = counter("event.dispatch.exception.TelemetryEvent")
        assertTrue(after > before)
    }

    @Test
    fun `duplicate registration increments duplicate registration counter`() {
        val before = counter("event.registration.duplicate.listener.TelemetryEvent")
        val listener = EventListener<TelemetryEvent>(condition = { true }, action = { true }, otherwiseAction = { })

        GameEventBus.on(TelemetryEvent::class.java, listener)
        GameEventBus.on(TelemetryEvent::class.java, listener)

        val after = counter("event.registration.duplicate.listener.TelemetryEvent")
        assertTrue(after > before)
    }

    private fun counter(name: String): Long {
        val snapshot = OperationalTelemetry.snapshot()
        @Suppress("UNCHECKED_CAST")
        val counters = snapshot["counters"] as Map<String, Long>
        return counters[name] ?: 0L
    }
}
