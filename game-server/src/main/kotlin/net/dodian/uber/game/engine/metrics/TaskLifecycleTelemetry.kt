package net.dodian.uber.game.engine.metrics

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

data class TaskLifecycleTelemetrySnapshot(
    val queuePressureByOwner: Map<String, Long>,
)

object TaskLifecycleTelemetry {
    private val queuePressureByOwner = ConcurrentHashMap<String, LongAdder>()

    @JvmStatic
    fun recordScheduled() {
        OperationalTelemetry.incrementCounter("task.lifecycle.scheduled")
    }

    @JvmStatic
    fun recordPending() {
        OperationalTelemetry.incrementCounter("task.lifecycle.pending")
    }

    @JvmStatic
    fun recordActive() {
        OperationalTelemetry.incrementCounter("task.lifecycle.active")
    }

    @JvmStatic
    fun recordCompleted() {
        OperationalTelemetry.incrementCounter("task.lifecycle.completed")
    }

    @JvmStatic
    fun recordCancelled(reason: String) {
        OperationalTelemetry.incrementCounter("task.lifecycle.cancelled")
        OperationalTelemetry.incrementCounter("task.lifecycle.cancelled.reason.$reason")
    }

    @JvmStatic
    fun recordFailure(exception: Throwable) {
        OperationalTelemetry.incrementCounter("task.lifecycle.failure.total")
        OperationalTelemetry.incrementCounter("task.lifecycle.failure.${exception.javaClass.simpleName}")
    }

    @JvmStatic
    fun recordQueuePressure(ownerType: String, queueSize: Int) {
        if (queueSize < 32) {
            return
        }
        queuePressureByOwner.computeIfAbsent(ownerType) { LongAdder() }.increment()
        OperationalTelemetry.incrementCounter("task.queue.pressure.$ownerType")
    }

    @JvmStatic
    fun snapshot(): TaskLifecycleTelemetrySnapshot {
        val byOwner = queuePressureByOwner.entries.associate { (ownerType, count) -> ownerType to count.sum() }
        return TaskLifecycleTelemetrySnapshot(queuePressureByOwner = byOwner)
    }
}
