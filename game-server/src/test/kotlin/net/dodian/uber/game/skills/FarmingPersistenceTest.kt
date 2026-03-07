package net.dodian.uber.game.skills

import com.google.gson.JsonPrimitive
import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.player.FarmingSegmentSnapshot
import net.dodian.uber.game.persistence.player.PlayerSaveEnvelope
import net.dodian.uber.game.persistence.player.PlayerSaveReason
import net.dodian.uber.game.persistence.player.PlayerSaveSegment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FarmingPersistenceTest {
    @Test
    fun `farming json snapshot cache initializes and refreshes`() {
        val farmingJson = FarmingJson()
        farmingJson.farmingLoad("")

        val compost = FarmingData.compostBin.values().first()
        val initial = farmingJson.farmingSaveSnapshot()
        farmingJson.getCompostData().get(compost.name).asJsonArray.set(1, JsonPrimitive(FarmingData.compostState.DONE.toString()))

        assertEquals(initial, farmingJson.farmingSaveSnapshot())

        val refreshed = farmingJson.refreshSaveSnapshot()

        assertNotEquals(initial, refreshed)
        assertTrue(refreshed.contains(FarmingData.compostState.DONE.toString()))
    }

    @Test
    fun `mark farming dirty refreshes snapshot and sets farming save mask`() {
        val client = testClient()
        val compost = FarmingData.compostBin.values().first()
        client.farmingJson.getCompostData().get(compost.name).asJsonArray.set(1, JsonPrimitive(FarmingData.compostState.DONE.toString()))

        client.markFarmingDirty()

        assertTrue(client.saveDirtyMask and PlayerSaveSegment.FARMING.mask != 0)
        assertTrue(client.farmingJson.farmingSaveSnapshot().contains(FarmingData.compostState.DONE.toString()))
        release(client)
    }

    @Test
    fun `compost interaction marks farming dirty and save envelope uses cached snapshot`() {
        val client = testClient()
        val compost = FarmingData.compostBin.values().first()
        val values = client.farmingJson.getCompostData().get(compost.name).asJsonArray
        values.set(0, JsonPrimitive(FarmingData.compost.COMPOST.toString()))
        values.set(1, JsonPrimitive(FarmingData.compostState.DONE.toString()))
        values.set(2, JsonPrimitive(15))
        values.set(3, JsonPrimitive(0))
        client.farmingJson.refreshSaveSnapshot()

        client.farming.run { client.interactBin(compost.objectId, 1) }

        assertTrue(client.saveDirtyMask and PlayerSaveSegment.FARMING.mask != 0)
        val envelope =
            PlayerSaveEnvelope.fromClient(
                client = client,
                sequence = 1L,
                reason = PlayerSaveReason.PERIODIC,
                updateProgress = false,
                finalSave = false,
                dirtyMask = PlayerSaveSegment.FARMING.mask,
            )
        val farming = envelope.segments.filterIsInstance<FarmingSegmentSnapshot>().single()
        assertEquals(client.farmingJson.farmingSaveSnapshot(), farming.farming)
        assertTrue(farming.farming.contains(FarmingData.compostState.OPEN.toString()))
        release(client)
    }

    @Test
    fun `scheduled patch growth marks farming dirty`() {
        val client = testClient()
        val patch = FarmingData.patches.values().first()
        val values = client.farmingJson.getPatchData().get(patch.name).asJsonArray
        values.set(1, JsonPrimitive(FarmingData.patchState.WEED.toString()))
        values.set(3, JsonPrimitive(1))
        values.set(4, JsonPrimitive(2))
        client.farmingJson.refreshSaveSnapshot()

        client.farming.run { client.updateFarming() }

        assertTrue(client.saveDirtyMask and PlayerSaveSegment.FARMING.mask != 0)
        assertEquals(0, values.get(3).asInt)
        assertEquals(0, values.get(4).asInt)
        release(client)
    }

    private fun testClient(): Client =
        Client(EmbeddedChannel(), 1).apply {
            validClient = true
            dbId = 42
            playerName = "farm-test"
            farmingJson.farmingLoad("")
            clearAllSaveDirty()
        }

    private fun release(client: Client) {
        (client.channel as EmbeddedChannel).finishAndReleaseAll()
    }
}
