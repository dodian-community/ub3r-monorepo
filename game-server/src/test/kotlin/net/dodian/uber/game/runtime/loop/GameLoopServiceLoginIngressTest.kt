package net.dodian.uber.game.runtime.loop

import java.util.concurrent.CopyOnWriteArrayList
import net.dodian.jobs.impl.ActionProcessor
import net.dodian.jobs.impl.EntityProcessor
import net.dodian.jobs.impl.ItemProcessor
import net.dodian.jobs.impl.ObjectProcess
import net.dodian.jobs.impl.PlunderDoor
import net.dodian.jobs.impl.ShopProcessor
import net.dodian.uber.game.runtime.phases.OutboundPacketProcessor
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameLoopServiceLoginIngressTest {

    @Test
    fun `idle ingress drains critical lane without running deferred jobs`() {
        GameThreadIngress.clearForTests()
        val events = CopyOnWriteArrayList<String>()
        val service = createService()

        try {
            GameThreadIngress.submitCritical("login-finalize") { events += "critical-1" }
            GameThreadIngress.submitCritical("login-finalize") { events += "critical-2" }
            GameThreadIngress.submitDeferred("login-post-init") { events += "deferred-1" }

            invoke(service, "runIdleIngress")

            assertEquals(listOf("critical-1", "critical-2"), events)

            invoke(service, "runTick")

            assertEquals(listOf("critical-1", "critical-2", "deferred-1"), events)
        } finally {
            GameThreadIngress.clearForTests()
        }
    }

    @Test
    fun `tick ingress preserves ordering within critical and deferred lanes`() {
        GameThreadIngress.clearForTests()
        val events = CopyOnWriteArrayList<String>()
        val service = createService()

        try {
            GameThreadIngress.submitCritical("login-finalize") { events += "critical-a" }
            GameThreadIngress.submitCritical("login-finalize") { events += "critical-b" }
            GameThreadIngress.submitDeferred("login-post-init") { events += "deferred-a" }
            GameThreadIngress.submitDeferred("generic-task") { events += "deferred-b" }

            invoke(service, "runTick")

            assertEquals(listOf("critical-a", "critical-b", "deferred-a", "deferred-b"), events)
            assertTrue(events.indexOf("critical-b") < events.indexOf("deferred-a"))
        } finally {
            GameThreadIngress.clearForTests()
        }
    }

    @Test
    fun `scheduled tick survives phase exception`() {
        val service =
            GameLoopService(
                entityProcessor = object : EntityProcessor() {
                    override fun runInboundPacketPhase() = Unit

                    override fun runNpcMainPhase(now: Long) = Unit

                    override fun runPlayerMainPhase() {
                        throw IllegalStateException("boom")
                    }

                    override fun runMovementFinalizePhase() = Unit

                    override fun runHousekeepingPhase(now: Long) = Unit
                },
                actionProcessor = object : ActionProcessor() { override fun run() = Unit },
                outboundPacketProcessor = object : OutboundPacketProcessor() { override fun run() = Unit },
                itemProcessor = object : ItemProcessor() { override fun run() = Unit },
                shopProcessor = object : ShopProcessor() { override fun run() = Unit },
                objectProcess = object : ObjectProcess() { override fun run() = Unit },
                plunderDoor = object : PlunderDoor() { override fun run() = Unit },
            )

        setRunning(service, true)
        assertDoesNotThrow { invoke(service, "runScheduledTick") }
    }

    @Test
    fun `idle ingress survives timer exception and continues draining`() {
        GameThreadTimers.clearForTests()
        val events = CopyOnWriteArrayList<String>()
        val service = createService()

        try {
            GameThreadTimers.schedule("explode", 0L, "test") { throw IllegalStateException("boom") }
            GameThreadTimers.schedule("follow-up", 0L, "test") { events += "ran" }

            setRunning(service, true)
            assertDoesNotThrow { invoke(service, "runIdleIngressScheduled") }

            assertEquals(listOf("ran"), events)
        } finally {
            GameThreadTimers.clearForTests()
        }
    }

    private fun createService(): GameLoopService =
        GameLoopService(
            entityProcessor = object : EntityProcessor() {
                override fun runInboundPacketPhase() = Unit

                override fun runNpcMainPhase(now: Long) = Unit

                override fun runPlayerMainPhase() = Unit

                override fun runMovementFinalizePhase() = Unit

                override fun runHousekeepingPhase(now: Long) = Unit
            },
            actionProcessor = object : ActionProcessor() { override fun run() = Unit },
            outboundPacketProcessor = object : OutboundPacketProcessor() { override fun run() = Unit },
            itemProcessor = object : ItemProcessor() { override fun run() = Unit },
            shopProcessor = object : ShopProcessor() { override fun run() = Unit },
            objectProcess = object : ObjectProcess() { override fun run() = Unit },
            plunderDoor = object : PlunderDoor() { override fun run() = Unit },
        )

    private fun invoke(service: GameLoopService, methodName: String) {
        val method = GameLoopService::class.java.getDeclaredMethod(methodName)
        method.isAccessible = true
        method.invoke(service)
    }

    private fun setRunning(service: GameLoopService, value: Boolean) {
        val field = GameLoopService::class.java.getDeclaredField("running")
        field.isAccessible = true
        val atomic = field.get(service) as java.util.concurrent.atomic.AtomicBoolean
        atomic.set(value)
    }
}
