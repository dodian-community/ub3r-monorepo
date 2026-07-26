package net.dodian.uber.game.engine.sync

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.sync.npc.StagedNpcSynchronizationService
import net.dodian.uber.game.engine.sync.player.StagedPlayerSynchronizationService
import net.dodian.uber.game.engine.systems.world.npc.NpcManager
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.item.ItemManager
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.testutil.RequiresCache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Fixed-byte regression harness for the STAGED player/NPC sync encoders (packets 81/65).
 *
 * These bytes are frozen snapshots of real encoder output, not re-derived per run — that is the
 * point. Before this file existed, the only guard against a sync-refactor regression was
 * "staged matches canonical" (see StagedPlayerSynchronizationServiceTest /
 * StagedNpcSynchronizationServiceTest), which stops working once the canonical/optimized modes
 * are deleted. This file is the replacement safety net for that deletion and everything after it.
 *
 * If a change intentionally alters wire bytes (e.g. appearance tickets skipping a block), update
 * the affected literal here deliberately — do not "fix the test" reflexively.
 */
class SyncGoldenBytesTest {
    private var previousItemManager: ItemManager? = null
    private var previousNpcManager: NpcManager? = null
    private val clients = mutableListOf<Client>()

    @BeforeEach
    fun setUp() {
        RequiresCache.assume()
        previousItemManager = Server.itemManager
        Server.itemManager = ItemManager(definitionLoader = { emptyMap() }, globalSpawnBootstrap = {})
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
        PlayerRegistry.players.fill(null)
        PlayerRegistry.playersOnline.clear()
        Server.itemManager = previousItemManager
        Server.npcManager = previousNpcManager
    }

    @Test
    fun `local admission of one nearby player`() {
        val viewer = client(1, "viewer", 3200, 3200)
        val subject = client(2, "subject", 3201, 3200)
        PlayerRegistry.players[1] = viewer
        PlayerRegistry.players[2] = subject

        assertEquals(
            "00002c01ffe010bb00ffffff00000000011200011a012601030122012a010e" +
                "020e05040003280333033303330333033303380000000bb0b4ea280a000000" +
                "000a0a0a3ff0000000000000000000",
            encodePlayer(viewer),
        )
    }

    @Test
    fun `retained local player with no changes emits only the skip bit`() {
        val viewer = client(1, "viewer", 3200, 3200)
        val subject = client(2, "subject", 3201, 3200)
        viewer.playerList[0] = subject
        viewer.playerListSize = 1
        viewer.playersUpdating.add(subject)
        PlayerRegistry.players[1] = viewer
        PlayerRegistry.players[2] = subject

        assertEquals("00bff8", encodePlayer(viewer))
    }

    @Test
    fun `retained local player removed when out of view`() {
        val viewer = client(1, "viewer", 3200, 3200)
        val subject = client(2, "subject", 3201, 3200)
        viewer.playerList[0] = subject
        viewer.playerListSize = 1
        viewer.playersUpdating.add(subject)
        PlayerRegistry.players[1] = viewer
        // subject intentionally NOT registered / moved far away -> not retained -> removal bits
        subject.moveTo(3400, 3400, 0)
        PlayerRegistry.players[2] = subject

        assertEquals("00fffe", encodePlayer(viewer))
    }

    @Test
    fun `teleporting viewer encodes self movement type three`() {
        val viewer = client(1, "viewer", 3200, 3200)
        PlayerRegistry.players[1] = viewer
        viewer.teleportTo(3210, 3215, 0)
        viewer.getNextPlayerMovement()

        assertEquals("e4dd9007ff", encodePlayer(viewer))
    }

    @Test
    fun `equipment change emits an appearance block on the shared add-local path`() {
        val viewer = client(1, "viewer", 3200, 3200)
        val subject = client(2, "subject", 3201, 3200)
        subject.equipment[Equipment.Slot.WEAPON.id] = 1277
        PlayerRegistry.players[1] = viewer
        PlayerRegistry.players[2] = subject

        assertEquals(
            "00002c01ffe010ba00ffffff00000006fd011200011a012601030122012a01" +
                "0e020e05040003280333033303330333033303380000000bb0b4ea280a0000" +
                "00000a0a0a3ff0000000000000000000",
            encodePlayer(viewer),
        )
    }

    @Test
    fun `single hit splat is encoded in the shared block`() {
        val viewer = client(1, "viewer", 3200, 3200)
        val subject = client(2, "subject", 3201, 3200)
        subject.dealDamage(null, 10, Entity.hitType.STANDARD)
        subject.getUpdateFlags().setRequired(UpdateFlag.HIT, true)
        PlayerRegistry.players[1] = viewer
        PlayerRegistry.players[2] = subject

        assertEquals(
            "00002c01ffe0300a2000bb00ffffff00000000011200011a012601030122012a" +
                "010e020e05040003280333033303330333033303380000000bb0b4ea280a000" +
                "000000a0a0a3ff0000000000000000000",
            encodePlayer(viewer),
        )
    }

    @Test
    fun `forced chat text is encoded in the shared block`() {
        val viewer = client(1, "viewer", 3200, 3200)
        val subject = client(2, "subject", 3201, 3200)
        subject.forcedChat = "hello"
        subject.getUpdateFlags().setRequired(UpdateFlag.FORCED_CHAT, true)
        PlayerRegistry.players[1] = viewer
        PlayerRegistry.players[2] = subject

        assertEquals(
            "00002c01ffe01468656c6c6f0abb00ffffff00000000011200011a01260103" +
                "0122012a010e020e05040003280333033303330333033303380000000bb0b4" +
                "ea280a000000000a0a0a3ff0000000000000000000",
            encodePlayer(viewer),
        )
    }

    @Test
    fun `npc add and movement round trip`() {
        val viewer = client(1, "npc-viewer", 3200, 3200)
        val npc = Npc(1, 1, Position(3201, 3200, 0), 0)
        Server.npcManager.npcMap[npc.slot] = npc

        assertEquals("00000100400020", encodeNpc(viewer))
    }

    @Test
    fun `npc hit splat is encoded in the fixed bit plane`() {
        val viewer = client(1, "npc-viewer", 3200, 3200)
        val npc = Npc(1, 1, Position(3201, 3200, 0), 0)
        npc.dealDamage(viewer, 10, Entity.hitType.STANDARD)
        Server.npcManager.npcMap[npc.slot] = npc

        assertEquals("0000010040003ffff050090000072000", encodeNpc(viewer))
    }

    @Test
    fun `retained npc with movement direction`() {
        val viewer = client(1, "npc-viewer", 3200, 3200)
        val npc = Npc(1, 1, Position(3201, 3200, 0), 0)
        Server.npcManager.npcMap[npc.slot] = npc
        viewer.localNpcs[0] = npc
        viewer.localNpcSize = 1
        npc.direction = 0

        assertEquals("01a4", encodeNpc(viewer))
    }

    private fun encodePlayer(viewer: Client): String {
        val service = StagedPlayerSynchronizationService()
        val plan = service.buildPlan(viewer)
        // Bit-packing leaves the final partial byte's unwritten low bits as whatever the backing
        // buffer previously held (never read by the client, which stops at the declared bit
        // count — but this test compares exact bytes). A JVM-heap Unpooled buffer is always
        // zero-initialized, unlike the pooled/thread-local buffers production code reuses, so the
        // assertion is deterministic regardless of what other tests ran before this one.
        val packet = ByteMessage.wrap(io.netty.buffer.Unpooled.buffer(8192))
        service.encode(viewer, plan, packet)
        val bytes = packet.toByteArray()
        packet.releaseAll()
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun encodeNpc(viewer: Client): String {
        val service = StagedNpcSynchronizationService()
        val plan = service.buildPlan(viewer)
        val packet = ByteMessage.wrap(io.netty.buffer.Unpooled.buffer(4096))
        service.encode(viewer, plan, packet)
        val bytes = packet.toByteArray()
        packet.releaseAll()
        return bytes.joinToString("") { "%02x".format(it) }
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
