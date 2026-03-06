package net.dodian.uber.game.runtime.sync

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.item.ItemManager
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.runtime.sync.cache.RootSynchronizationCache
import net.dodian.uber.game.runtime.sync.player.PlayerChunkActivityIndex
import net.dodian.uber.game.runtime.sync.player.PlayerSyncRevisionIndex
import net.dodian.uber.game.runtime.sync.playerinfo.RootPlayerInfoService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RootPlayerInfoTeleportSyncTest {

    @Test
    fun `same-region teleport reinserts local for observer without forcing observer rebuild`() {
        val viewer = registerClient(1)
        val teleporter = registerClient(2)
        val originalUpdateRunning = Server.updateRunning
        val originalItemManager = Server.itemManager

        try {
            Server.updateRunning = false
            Server.itemManager = testItemManager()

            runRootSync(listOf(viewer, teleporter), 1L)
            drainOutbound(viewer)
            drainOutbound(teleporter)

            teleporter.transport(Position(3209, 3205, 0))
            teleporter.getNextPlayerMovement()

            val cycle = runRootSync(listOf(viewer, teleporter), 2L)
            val viewerOutbound = drainSingleOutbound(viewer)
            val teleporterOutbound = drainSingleOutbound(teleporter)
            val viewerPayload = viewerOutbound.toByteArray()
            val teleporterPayload = teleporterOutbound.toByteArray()

            assertEquals(81, viewerOutbound.opcode)
            assertEquals(0, readBits(viewerPayload, 0, 1))
            assertEquals(1, readBits(viewerPayload, 1, 8))
            assertEquals(1, readBits(viewerPayload, 9, 1))
            assertEquals(3, readBits(viewerPayload, 10, 2))
            assertEquals(1, cycle.playerIncrementalAdmissionCount)
            assertEquals(1, cycle.playerFullRebuildCount)
            assertEquals(1, cycle.playerTeleportReinsertCount)
            assertEquals(1, cycle.playerTeleportReinsertSentCount)
            assertEquals(0, cycle.playerTeleportReinsertDeferredCount)
            assertEquals(1, cycle.playerLocalRemovalCount)
            assertEquals(1, cycle.playerLocalAdditionSentCount)
            assertEquals(1, viewer.playerListSize)
            assertSame(teleporter, viewer.playerList[0])

            assertEquals(81, teleporterOutbound.opcode)
            assertEquals(1, readBits(teleporterPayload, 0, 1))
            assertEquals(3, readBits(teleporterPayload, 1, 2))

            viewerOutbound.releaseAll()
            teleporterOutbound.releaseAll()
        } finally {
            Server.updateRunning = originalUpdateRunning
            Server.itemManager = originalItemManager
            cleanupClient(viewer)
            cleanupClient(teleporter)
        }
    }

    @Test
    fun `walking local stays on retained update path without teleport reinsert`() {
        val viewer = registerClient(1)
        val walker = registerClient(2)
        val originalUpdateRunning = Server.updateRunning
        val originalItemManager = Server.itemManager

        try {
            Server.updateRunning = false
            Server.itemManager = testItemManager()

            runRootSync(listOf(viewer, walker), 1L)
            drainOutbound(viewer)
            drainOutbound(walker)

            walker.addToWalkingQueue(walker.currentX + 1, walker.currentY)
            walker.getNextPlayerMovement()

            val cycle = runRootSync(listOf(viewer, walker), 2L)
            val viewerOutbound = drainSingleOutbound(viewer)
            val payload = viewerOutbound.toByteArray()

            assertEquals(81, viewerOutbound.opcode)
            assertEquals(0, readBits(payload, 0, 1))
            assertEquals(1, readBits(payload, 1, 8))
            assertEquals(1, readBits(payload, 9, 1))
            assertEquals(1, readBits(payload, 10, 2))
            assertEquals(0, cycle.playerTeleportReinsertCount)
            assertEquals(0, cycle.playerTeleportReinsertSentCount)
            assertEquals(0, cycle.playerLocalAdditionSentCount)
            assertEquals(1, viewer.playerListSize)
            assertSame(walker, viewer.playerList[0])
            assertTrue(walker.primaryDirection != -1)

            viewerOutbound.releaseAll()
            drainOutbound(walker)
        } finally {
            Server.updateRunning = originalUpdateRunning
            Server.itemManager = originalItemManager
            cleanupClient(viewer)
            cleanupClient(walker)
        }
    }

    private fun registerClient(slot: Int): Client =
        Client(EmbeddedChannel(), slot).apply {
            isActive = true
            loaded = true
            setPlayerName("teleport-$slot")
            moveTo(3200 + slot, 3200, 0)
            PlayerHandler.players[slot] = this
        }

    private fun cleanupClient(client: Client) {
        PlayerHandler.players[client.slot] = null
        drainOutbound(client)
        embeddedChannel(client).finishAndReleaseAll()
    }

    private fun runRootSync(players: List<Client>, tick: Long): SynchronizationCycle {
        val revisionIndex = PlayerSyncRevisionIndex()
        val activityIndex = PlayerChunkActivityIndex()
        revisionIndex.rebuild(players, tick, activityIndex)
        val cycle =
            SynchronizationCycle(
                tick = tick,
                rootCache = RootSynchronizationCache(),
                viewportIndex = null,
                playerRevisionIndex = revisionIndex,
                playerActivityIndex = activityIndex,
            )
        SynchronizationContext.setCurrent(cycle)
        try {
            RootPlayerInfoService.INSTANCE.sync(players)
            return cycle
        } finally {
            SynchronizationContext.clear()
        }
    }

    private fun drainOutbound(client: Client) {
        client.flushOutbound()
        val channel = embeddedChannel(client)
        while (true) {
            val outbound = channel.readOutbound<Any>() ?: break
            if (outbound is ByteMessage) {
                outbound.releaseAll()
            }
        }
    }

    private fun drainSingleOutbound(client: Client): ByteMessage {
        client.flushOutbound()
        val channel = embeddedChannel(client)
        val outbound = channel.readOutbound<Any>()
        assertNotNull(outbound)
        assertTrue(outbound is ByteMessage)
        assertNull(channel.readOutbound<Any>())
        return outbound as ByteMessage
    }

    private fun embeddedChannel(client: Client): EmbeddedChannel = client.channel as EmbeddedChannel

    private fun testItemManager(): ItemManager =
        object : ItemManager() {
            override fun loadGlobalItems() = Unit
            override fun loadItems() = Unit
        }

    private fun readBits(payload: ByteArray, startBit: Int, bitCount: Int): Int {
        var value = 0
        repeat(bitCount) { bit ->
            val absoluteBit = startBit + bit
            val byteIndex = absoluteBit / 8
            val bitIndex = 7 - (absoluteBit % 8)
            val bitValue = (payload[byteIndex].toInt() ushr bitIndex) and 1
            value = (value shl 1) or bitValue
        }
        return value
    }
}
