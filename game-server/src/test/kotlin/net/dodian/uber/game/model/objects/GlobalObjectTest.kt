package net.dodian.uber.game.model.objects

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.codec.ByteMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val SET_MAP_OPCODE = 85
private const val CLEAR_OBJECT_OPCODE = 101
private const val PLACE_OBJECT_OPCODE = 151

class GlobalObjectTest {
    private val clients = mutableListOf<Client>()
    private val addedObjects = mutableListOf<WorldObject>()

    @AfterEach
    fun tearDown() {
        addedObjects.forEach { GlobalObject.getGlobalObject().remove(it) }
        addedObjects.clear()
        clients.forEach {
            it.saveNeeded = false
            it.destruct()
        }
        clients.clear()
        PlayerRegistry.players.fill(null)
        PlayerRegistry.playersOnline.clear()
    }

    @Test
    fun `per-viewer refresh sends only to that viewer, not to every nearby player`() {
        val viewer = client(1, "viewer", 3200, 3200)
        val bystander = client(2, "bystander", 3200, 3200)

        val worldObject = WorldObject(id = 1234, x = 3200, y = 3200, z = 0, type = 10, face = 0)
        addedObjects += worldObject
        GlobalObject.addGlobalObject(worldObject, 60_000)
        // addGlobalObject's own broadcast (a real "object appeared" event) is correct and expected
        // to reach both nearby players - drain it before testing the per-viewer refresh in isolation.
        drainAll(viewer, bystander)

        GlobalObject.updateObject(viewer)

        val setMap = (viewer.channel as EmbeddedChannel).readOutbound<ByteMessage>()
        assertEquals(SET_MAP_OPCODE, setMap.opcode)
        val clearObject = (viewer.channel as EmbeddedChannel).readOutbound<ByteMessage>()
        assertEquals(CLEAR_OBJECT_OPCODE, clearObject.opcode)
        val placeObject = (viewer.channel as EmbeddedChannel).readOutbound<ByteMessage>()
        assertEquals(PLACE_OBJECT_OPCODE, placeObject.opcode)
        assertNull((viewer.channel as EmbeddedChannel).readOutbound<ByteMessage>())

        assertNull(
            (bystander.channel as EmbeddedChannel).readOutbound<ByteMessage>(),
            "a per-viewer refresh for `viewer` must not broadcast to other nearby players",
        )
    }

    @Test
    fun `sweepExpired reverts and broadcasts to every nearby viewer, then removes the object`() {
        val viewer = client(1, "viewer", 3200, 3200)
        val bystander = client(2, "bystander", 3200, 3200)

        val worldObject = WorldObject(id = 1234, x = 3200, y = 3200, z = 0, type = 10, face = 0, oldId = 5678)
        addedObjects += worldObject
        GlobalObject.addGlobalObject(worldObject, 60_000)
        drainAll(viewer, bystander)
        // Force expiry without waiting on a real timer.
        worldObject.setAttachment(System.currentTimeMillis() - 1)

        GlobalObject.sweepExpired()

        // A genuine expiry is a real world event: both nearby viewers should learn about it
        // (oldId=5678, not -1, so a PlaceObject follows the ClearObject for the reverted object).
        assertEquals(SET_MAP_OPCODE, (viewer.channel as EmbeddedChannel).readOutbound<ByteMessage>().opcode)
        assertEquals(CLEAR_OBJECT_OPCODE, (viewer.channel as EmbeddedChannel).readOutbound<ByteMessage>().opcode)
        assertEquals(PLACE_OBJECT_OPCODE, (viewer.channel as EmbeddedChannel).readOutbound<ByteMessage>().opcode)
        assertEquals(SET_MAP_OPCODE, (bystander.channel as EmbeddedChannel).readOutbound<ByteMessage>().opcode)
        assertEquals(CLEAR_OBJECT_OPCODE, (bystander.channel as EmbeddedChannel).readOutbound<ByteMessage>().opcode)
        assertEquals(PLACE_OBJECT_OPCODE, (bystander.channel as EmbeddedChannel).readOutbound<ByteMessage>().opcode)

        assertFalse(GlobalObject.getGlobalObject().contains(worldObject))
    }

    @Test
    fun `global objects at the same tile on different planes keep separate identity`() {
        val lower = WorldObject(id = 1234, x = 3200, y = 3200, z = 0, type = 10, face = 0)
        val upper = WorldObject(id = 5678, x = 3200, y = 3200, z = 1, type = 10, face = 0)
        addedObjects += lower
        addedObjects += upper

        assertTrue(GlobalObject.addGlobalObject(lower, 60_000))
        assertTrue(GlobalObject.addGlobalObject(upper, 60_000))
        assertSame(lower, GlobalObject.getGlobalObject(3200, 3200, 0))
        assertSame(upper, GlobalObject.getGlobalObject(3200, 3200, 1))
    }

    private fun drainAll(vararg players: Client) {
        players.forEach { player ->
            val channel = player.channel as EmbeddedChannel
            while (channel.readOutbound<ByteMessage>() != null) {
                // drain
            }
        }
    }

    private fun client(slot: Int, name: String, x: Int, y: Int): Client =
        Client(EmbeddedChannel(), slot).apply {
            playerName = name
            moveTo(x, y, 0)
            initialized = true
            isActive = true
            PlayerRegistry.players[slot] = this
            clients += this
        }
}
