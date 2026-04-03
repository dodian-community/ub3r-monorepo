package net.dodian.uber.game.systems.world.farming

import com.google.gson.JsonPrimitive
import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.content.skills.farming.FarmingData
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FarmingRuntimeServiceTest {
    @AfterEach
    fun tearDown() {
        PlayerRegistry.playersOnline.clear()
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
