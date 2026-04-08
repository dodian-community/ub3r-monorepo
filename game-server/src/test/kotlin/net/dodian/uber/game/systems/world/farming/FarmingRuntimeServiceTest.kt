package net.dodian.uber.game.systems.world.farming

import com.google.gson.JsonPrimitive
import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.content.skills.farming.FarmingData
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import net.dodian.uber.game.tasks.TickTasks
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FarmingRuntimeServiceTest {
    @AfterEach
    fun tearDown() {
        GameTaskRuntime.clear()
        PlayerRegistry.playersOnline.clear()
    }

    @Test
    fun `tick pilot startup is idempotent`() {
        val runtime = FarmingRuntimeService()

        val started = runtime.ensureTickPilotStarted()
        val startedAgain = runtime.ensureTickPilotStarted()

        assertTrue(started)
        assertFalse(startedAgain)
    }

    @Test
    fun `codec snapshot exposes typed patch and compost slots`() {
        val client = testClient(slot = 1, key = 1001L)
        activatePatchWork(client)

        val snapshot = FarmingPersistenceCodec.snapshot(client)

        assertTrue(snapshot.patchSlots.isNotEmpty())
        assertTrue(snapshot.compostBins.isNotEmpty())
        val activePatch = snapshot.patchSlots.first { it.itemId != -1 }
        assertEquals(FarmingData.patchState.GROWING.toString(), activePatch.state)
    }

    @Test
    fun `runtime schedules and runs only active farming players`() {
        val runtime = FarmingRuntimeService()
        val now = System.currentTimeMillis()

        val active = testClient(slot = 2, key = 2002L)
        activatePatchWork(active)

        val idle = testClient(slot = 3, key = 3003L)

        runtime.onLogin(active, now)
        runtime.onLogin(idle, now)

        val stats = runtime.runDue(now)

        assertEquals(1, stats.duePlayers)
        assertEquals(1, stats.processedPlayers)
    }

    @Test
    fun `deferred login catch-up latency metric is recorded when backlog settles`() {
        val runtime = FarmingRuntimeService()
        val client = testClient(slot = 4, key = 4004L)
        activatePatchWork(client)
        val compost = FarmingData.compostBin.values().first()
        val compostArray = client.farmingJson.getCompostData().get(compost.name).asJsonArray
        compostArray.set(1, JsonPrimitive(FarmingData.compostState.FILLED.toString()))
        compostArray.set(2, JsonPrimitive(1))
        val now = 10_000_000L
        val pulseMs = 300_000L

        client.farmingJson.lastGlobalPulseAtMillis = now - (20 * pulseMs)
        GameCycleClock.syncTo(200)
        runtime.recordDeferredLoginCatchUpStart(client, TickTasks.gameClock())

        runtime.onLogin(client, now)
        var pollNow = now
        repeat(32) {
            if (runtime.deferredLoginCatchUpLatencySnapshot().isNotEmpty()) {
                return@repeat
            }
            GameCycleClock.advance()
            runtime.runDue(pollNow)
            pollNow += pulseMs
        }
        val metrics = runtime.deferredLoginCatchUpLatencySnapshot()
        assertTrue(metrics.isNotEmpty())
        assertEquals(1, metrics.values.sum())
    }

    private fun testClient(slot: Int, key: Long): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.dbId = key.toInt()
        client.farmingJson.farmingLoad("")
        PlayerRegistry.playersOnline[key] = client
        return client
    }

    private fun activatePatchWork(client: Client) {
        val patch = FarmingData.patches.values().first()
        val patchArray = client.farmingJson.getPatchData().get(patch.name).asJsonArray
        patchArray.set(0, JsonPrimitive(FarmingData.allotmentPatch.POTATO.seed))
        patchArray.set(1, JsonPrimitive(FarmingData.patchState.GROWING.toString()))
        patchArray.set(3, JsonPrimitive(1))
    }
}
