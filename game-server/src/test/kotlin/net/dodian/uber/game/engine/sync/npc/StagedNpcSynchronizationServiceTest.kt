package net.dodian.uber.game.engine.sync.npc

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.systems.world.npc.NpcManager
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.testutil.RequiresCache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StagedNpcSynchronizationServiceTest {
    private var previousNpcManager: NpcManager? = null
    private val channels = mutableListOf<EmbeddedChannel>()
    private val clients = mutableListOf<Client>()

    @BeforeEach
    fun setUp() {
        RequiresCache.assume()
        previousNpcManager = Server.npcManager
        Server.npcManager = NpcManager()
    }

    @AfterEach
    fun tearDown() {
        clients.forEach {
            it.saveNeeded = false
            it.destruct()
        }
        clients.clear()
        channels.forEach(EmbeddedChannel::close)
        channels.clear()
        Server.npcManager = previousNpcManager
    }

    @Test
    fun `staged npc encoding does not commit until commit is called`() {
        // Exact byte layout for this scenario is locked by SyncGoldenBytesTest; this test covers
        // the plan/encode/commit staging contract instead.
        val viewer = client(1)
        val npc = Npc(1, 1, Position(3201, 3200, 0), 0)
        Server.npcManager.npcMap[npc.slot] = npc
        val service = StagedNpcSynchronizationService()
        val plan = service.buildPlan(viewer)
        val staged = ByteMessage.raw(4096)
        service.encode(viewer, plan, staged)
        staged.releaseAll()

        assertEquals(0, viewer.localNpcSize)
        service.commit(viewer, plan)
        assertEquals(1, viewer.localNpcSize)
        assertSame(npc, viewer.localNpcs[0])
    }

    @Test
    fun `npc encoding failure leaves viewer state uncommitted`() {
        val viewer = client(1)
        val invalidSlotNpc = Npc(16384, 1, Position(3201, 3200, 0), 0)
        Server.npcManager.npcMap[invalidSlotNpc.slot] = invalidSlotNpc
        val service = StagedNpcSynchronizationService()
        val plan = service.buildPlan(viewer)
        val packet = ByteMessage.raw(4096)

        assertThrows(IllegalArgumentException::class.java) { service.encode(viewer, plan, packet) }
        packet.releaseAll()
        assertEquals(0, viewer.localNpcSize)
    }

    @Test
    fun `required npc enqueue rejection leaves viewer state uncommitted`() {
        val viewer = client(1)
        val npc = Npc(1, 1, Position(3201, 3200, 0), 0)
        Server.npcManager.npcMap[npc.slot] = npc
        repeat(1024) { viewer.send(ByteMessage.raw(1)) }

        val result = StagedNpcSynchronizationService().synchronize(viewer)

        assertTrue(!result.accepted)
        assertEquals(0, viewer.localNpcSize)
    }

    @Test
    fun `respawn schedules an appearance refresh`() {
        val npc = Npc(1, 1, Position(3201, 3200, 0), 0)
        npc.updateFlags.clear()

        npc.respawn()

        assertTrue(npc.updateFlags.isRequired(UpdateFlag.APPEARANCE))
    }

    @Test
    fun `unchanged focus does not schedule a repeated face coordinate update`() {
        val npc = Npc(1, 1, Position(3201, 3200, 0), -1)
        npc.updateFlags.clear()
        npc.setFocus(3205, 3200)
        npc.applyFocus(3205, 3200)
        assertTrue(npc.updateFlags.isRequired(UpdateFlag.FACE_COORDINATE))

        npc.updateFlags.clear()
        npc.setFocus(3205, 3200)
        npc.applyFocus(3205, 3200)

        assertTrue(!npc.updateFlags.isRequired(UpdateFlag.FACE_COORDINATE))
    }

    @Test
    fun `unsupported npc flags do not create an empty update block or disconnect the viewer`() {
        val viewer = client(1)
        val npc = Npc(1, 1, Position(3201, 3200, 0), 0)
        npc.updateFlags.setRequired(UpdateFlag.FORCED_MOVEMENT, true)
        Server.npcManager.npcMap[npc.slot] = npc

        val result = StagedNpcSynchronizationService().synchronize(viewer)

        assertTrue(result.accepted)
        assertEquals(1, viewer.localNpcSize)
        assertTrue(!viewer.disconnected)
    }

    private fun client(slot: Int): Client {
        val channel = EmbeddedChannel()
        channels += channel
        return Client(channel, slot).apply {
            playerName = "viewer$slot"
            moveTo(3200, 3200, 0)
            loaded = true
            initialized = true
            isActive = true
            setSynchronizationReady(true)
            clients += this
        }
    }
}
