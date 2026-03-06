package net.dodian.uber.game.runtime.loop

import java.util.concurrent.CopyOnWriteArrayList
import net.dodian.jobs.impl.ActionProcessor
import net.dodian.jobs.impl.EntityProcessor
import net.dodian.jobs.impl.ItemProcessor
import net.dodian.jobs.impl.ObjectProcess
import net.dodian.jobs.impl.OutboundPacketProcessor
import net.dodian.jobs.impl.PlunderDoor
import net.dodian.jobs.impl.ShopProcessor
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameLoopServiceLoginIngressTest {

    @Test
    fun `login finalization queued during npc phase drains before player main`() {
        LoginFinalizationQueue.drain(Int.MAX_VALUE)
        val events = CopyOnWriteArrayList<String>()
        val entityProcessor =
            object : EntityProcessor() {
                override fun runInboundPacketPhase() {
                    events += "inbound"
                }

                override fun runNpcMainPhase(now: Long) {
                    events += "npc"
                    LoginFinalizationQueue.submit("test-finalize") {
                        events += "login-finalize"
                    }
                }

                override fun runPlayerMainPhase() {
                    assertTrue(events.contains("login-finalize"))
                    events += "player"
                }

                override fun runMovementFinalizePhase() {
                    events += "movement"
                }

                override fun runHousekeepingPhase(now: Long) {
                    events += "housekeeping"
                }
            }

        val service =
            GameLoopService(
                entityProcessor = entityProcessor,
                actionProcessor = object : ActionProcessor() { override fun run() = Unit },
                outboundPacketProcessor = object : OutboundPacketProcessor() { override fun run() = Unit },
                itemProcessor = object : ItemProcessor() { override fun run() = Unit },
                shopProcessor = object : ShopProcessor() { override fun run() = Unit },
                objectProcess = object : ObjectProcess() { override fun run() = Unit },
                plunderDoor = object : PlunderDoor() { override fun run() = Unit },
            )

        try {
            runSingleTick(service)
            assertTrue(events.indexOf("npc") >= 0)
            assertTrue(events.indexOf("login-finalize") > events.indexOf("npc"))
            assertTrue(events.indexOf("login-finalize") < events.indexOf("player"))
        } finally {
            LoginFinalizationQueue.drain(Int.MAX_VALUE)
        }
    }

    private fun runSingleTick(service: GameLoopService) {
        val method = GameLoopService::class.java.getDeclaredMethod("runTick")
        method.isAccessible = true
        method.invoke(service)
    }
}
