package net.dodian.uber.game.model.item

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.engine.systems.zone.ZoneUpdateBus
import net.dodian.uber.game.item.ItemManager
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.chunk.ChunkManager
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.codec.ByteMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val SET_MAP_OPCODE = 85
private const val CREATE_GROUND_ITEM_OPCODE = 44

class GroundItemZoneDeliveryTest {
    private var previousChunkManager: ChunkManager? = null
    private var previousItemManager: ItemManager? = null
    private val clients = mutableListOf<Client>()

    @BeforeEach
    fun setUp() {
        previousChunkManager = Server.chunkManager
        Server.chunkManager = ChunkManager()
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
        Server.chunkManager = previousChunkManager
        Server.itemManager = previousItemManager
    }

    @Test
    fun `nearby viewer is notified, the dropper is not double-sent, and a far viewer is untouched`() {
        val viewer = client(1, "viewer", 3200, 3200, dbId = 100)
        val dropper = client(2, "dropper", 3200, 3200, dbId = 200)
        val farAway = client(3, "far-away", 3200 + 200, 3200, dbId = 300)
        PlayerRegistry.players[dropper.slot] = dropper

        val item = GroundItem(Position(3200, 3200, 0), 995, 1, dropper.slot, -1)
        // the constructor's sendOwnerCreate already queued the dropper a SetMap + CreateGroundItem
        // pair - drain both before asserting on itemDisplay()'s output
        dropper.flushOutbound()
        (dropper.channel as EmbeddedChannel).readOutbound<ByteMessage>()
        (dropper.channel as EmbeddedChannel).readOutbound<ByteMessage>()

        // Mirrors ItemProcessor.kt: personal drops start invisible to everyone but the dropper,
        // then flip visible=true and call itemDisplay() once the personal-view timer expires.
        item.visible = true
        item.itemDisplay()
        ZoneUpdateBus.flush(listOf(viewer, dropper, farAway))
        listOf(viewer, dropper, farAway).forEach { it.flushOutbound() }

        val viewerSetMap = (viewer.channel as EmbeddedChannel).readOutbound<ByteMessage>()
        assertEquals(SET_MAP_OPCODE, viewerSetMap.opcode)
        val viewerBody = (viewer.channel as EmbeddedChannel).readOutbound<ByteMessage>()
        assertEquals(CREATE_GROUND_ITEM_OPCODE, viewerBody.opcode)
        assertNull((viewer.channel as EmbeddedChannel).readOutbound<ByteMessage>())

        assertNull((dropper.channel as EmbeddedChannel).readOutbound<ByteMessage>(), "owner must not be double-sent their own drop")
        assertNull((farAway.channel as EmbeddedChannel).readOutbound<ByteMessage>(), "a viewer outside chunk range must receive nothing")
    }

    // loaded=true is required: Player.isSynchronizationReady() (checked by PacketZoneDelta.appliesTo)
    // requires it. That in turn makes Client.send() queue onto outboundSessionQueue instead of
    // writing the channel directly (shouldQueueOutbound() = isActive && loaded && !disconnected),
    // so flushOutbound() must be called before messages show up via channel.readOutbound().
    private fun client(slot: Int, name: String, x: Int, y: Int, dbId: Int): Client =
        Client(EmbeddedChannel(), slot).apply {
            playerName = name
            moveTo(x, y, 0)
            initialized = true
            loaded = true
            isActive = true
            setSynchronizationReady(true)
            this.dbId = dbId
            syncChunkMembership()
            clients += this
        }
}
