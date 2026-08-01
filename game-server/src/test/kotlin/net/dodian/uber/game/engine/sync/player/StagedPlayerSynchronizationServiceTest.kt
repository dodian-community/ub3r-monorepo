package net.dodian.uber.game.engine.sync.player

import io.netty.channel.embedded.EmbeddedChannel
import kotlin.random.Random
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.item.ItemManager
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.codec.ByteMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StagedPlayerSynchronizationServiceTest {
    private val clients = mutableListOf<Client>()
    private var previousItemManager: ItemManager? = null

    @BeforeEach
    fun setUp() {
        previousItemManager = Server.itemManager
        Server.itemManager = ItemManager(definitionLoader = { emptyMap() }, globalSpawnBootstrap = {})
    }

    @AfterEach
    fun tearDown() {
        clients.forEach {
            it.saveNeeded = false
            it.destruct()
        }
        clients.clear()
        PlayerRegistry.players.fill(null)
        PlayerRegistry.playersOnline.clear()
        Server.itemManager = previousItemManager
    }

    @Test
    fun `encoding a new local admission does not commit until commit is called`() {
        // Exact byte layout for this scenario is locked by SyncGoldenBytesTest; this test covers
        // the plan/encode/commit staging contract instead.
        val viewer = client(1, "viewer", 3200, 3200)
        val subject = client(2, "subject", 3201, 3200)
        PlayerRegistry.players[1] = viewer
        PlayerRegistry.players[2] = subject
        val service = StagedPlayerSynchronizationService()
        val plan = service.buildPlan(viewer)
        val staged = ByteMessage.raw(4096)
        service.encode(viewer, plan, staged)
        staged.releaseAll()

        assertEquals(0, viewer.playerListSize, "encoding must not commit viewer state")
        service.commit(viewer, plan)
        assertEquals(1, viewer.playerListSize)
        assertSame(subject, viewer.playerList[0])
    }

    @Test
    fun `stale slot replacement remains unchanged until commit`() {
        val viewer = client(1, "viewer", 3200, 3200)
        val old = client(2, "old", 3201, 3200)
        val replacement = client(2, "replacement", 3201, 3200)
        viewer.playerList[0] = old
        viewer.playerListSize = 1
        viewer.playersUpdating.add(old)
        PlayerRegistry.players[1] = viewer
        PlayerRegistry.players[2] = replacement

        val service = StagedPlayerSynchronizationService()
        val plan = service.buildPlan(viewer)
        val packet = ByteMessage.raw(4096)
        service.encode(viewer, plan, packet)
        packet.releaseAll()

        assertSame(old, viewer.playerList[0])
        assertEquals(1, viewer.playerListSize)
        service.commit(viewer, plan)
        assertSame(replacement, viewer.playerList[0])
    }

    @Test
    fun `all ninety nine visible players fit one staged admission`() {
        val viewer = client(1, "viewer", 3200, 3200)
        PlayerRegistry.players[1] = viewer
        for (slot in 2..100) {
            PlayerRegistry.players[slot] = client(slot, "p$slot", 3200 + (slot % 8), 3200 + (slot % 7))
        }

        val service = StagedPlayerSynchronizationService()
        val plan = service.buildPlan(viewer)
        assertEquals(99, plan.additionCount)
        assertEquals(99, plan.nextCount)
        val packet = ByteMessage.raw(65535)
        service.encode(viewer, plan, packet)
        assertTrue(packet.buffer.writerIndex() in 1..65535)
        packet.releaseAll()
    }

    @Test
    fun `idle viewer admits a player who appears in a later tick`() {
        val viewer = client(1, "viewer", 3200, 3200)
        PlayerRegistry.players[1] = viewer
        val service = StagedPlayerSynchronizationService()

        // Tick 1: viewer is alone and idle - fast path takes over, nothing to admit yet.
        val first = service.synchronize(viewer)
        assertTrue(first.accepted)
        assertEquals(0, viewer.playerListSize)

        // Tick 2: a new player appears nearby without the viewer moving or getting any update flag.
        val newcomer = client(2, "newcomer", 3201, 3200)
        PlayerRegistry.players[2] = newcomer

        val second = service.synchronize(viewer)
        assertTrue(second.accepted)
        assertEquals(1, viewer.playerListSize, "idle viewer must still admit a newly-visible player")
        assertSame(newcomer, viewer.playerList[0])
        assertTrue(viewer.playersUpdating.contains(newcomer))
    }

    @Test
    fun `idle viewer removes a local player after that player teleports`() {
        val viewer = client(1, "viewer", 3200, 3200)
        val subject = client(2, "subject", 3201, 3200)
        PlayerRegistry.players[1] = viewer
        PlayerRegistry.players[2] = subject
        val service = StagedPlayerSynchronizationService()

        assertTrue(service.synchronize(viewer).accepted)
        assertEquals(1, viewer.playerListSize)

        subject.transport(Position(3400, 3400, 0))
        subject.getNextPlayerMovement()

        assertTrue(service.synchronize(viewer).accepted)
        assertEquals(0, viewer.playerListSize, "teleported locals must be removed, not frozen in the idle fast path")
    }

    @Test
    fun `idle viewer removes a local player who becomes invisible`() {
        val viewer = client(1, "viewer", 3200, 3200)
        val subject = client(2, "subject", 3201, 3200)
        PlayerRegistry.players[1] = viewer
        PlayerRegistry.players[2] = subject
        val service = StagedPlayerSynchronizationService()

        assertTrue(service.synchronize(viewer).accepted)
        assertEquals(1, viewer.playerListSize)

        subject.invis = true
        assertTrue(service.synchronize(viewer).accepted)
        assertEquals(0, viewer.playerListSize, "invisible locals must be removed for ordinary viewers")
    }

    @Test
    fun `required enqueue rejection leaves viewer local state uncommitted`() {
        val viewer = client(1, "viewer", 3200, 3200)
        val subject = client(2, "subject", 3201, 3200)
        PlayerRegistry.players[1] = viewer
        PlayerRegistry.players[2] = subject
        repeat(1024) { viewer.send(ByteMessage.raw(1)) }

        val result = StagedPlayerSynchronizationService().synchronize(viewer)

        assertTrue(!result.accepted)
        assertEquals(0, viewer.playerListSize)
        assertTrue(viewer.playersUpdating.isEmpty)
    }

    @Test
    fun `randomized local sets commit to internally consistent viewer state`() {
        // No canonical encoder to cross-check against anymore (deleted). Instead this exercises a
        // wide variety of membership/flag combinations and verifies the staged commit never leaves
        // the viewer's local-list bookkeeping inconsistent — the exact invariant that caused sync
        // desyncs historically (see PlayerSyncInvariantValidator).
        repeat(24) { seed ->
            encodeAndCommitScenario(seed)
            PlayerRegistry.players.fill(null)
            PlayerRegistry.playersOnline.clear()
        }
    }

    private fun encodeAndCommitScenario(seed: Int) {
        val random = Random(seed)
        val viewer = client(1, "viewer-$seed", 3200, 3200)
        PlayerRegistry.players[1] = viewer
        val subjects = (2..14).associateWith { slot ->
            client(slot, "subject-$slot", 3200 + slot - 1, 3200)
        }
        val previousCount = random.nextInt(0, 6)
        repeat(previousCount) { index ->
            val subject = subjects.getValue(index + 2)
            viewer.playerList[index] = subject
            viewer.playersUpdating.add(subject)
        }
        viewer.playerListSize = previousCount
        subjects.forEach { (slot, subject) ->
            if (slot > previousCount + 1 || random.nextBoolean()) {
                PlayerRegistry.players[slot] = subject
            }
            if (random.nextInt(4) == 0) subject.faceTarget(random.nextInt(0, 2047))
        }

        val service = StagedPlayerSynchronizationService()
        val before = PlayerSyncInvariantValidator.snapshot(viewer)
        val plan = service.buildPlan(viewer)
        val packet = ByteMessage.raw(8192)
        service.encode(viewer, plan, packet)
        assertTrue(packet.buffer.writerIndex() > 0, "seed=$seed produced an empty packet")
        packet.releaseAll()
        service.commit(viewer, plan)
        PlayerSyncInvariantValidator.validateViewerLocals(viewer, before)
    }

    private fun client(slot: Int, name: String, x: Int, y: Int): Client =
        Client(EmbeddedChannel(), slot).apply {
            playerName = name
            moveTo(x, y, 0)
            loaded = true
            initialized = true
            isActive = true
            setSynchronizationReady(true)
            clients += this
        }
}
