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

class RootPlayerInfoPostTeleportMovementTest {

    @Test
    fun `observer keeps receiving walking updates after same-region teleport`() {
        val viewer = registerClient(1)
        val teleporter = registerClient(2)
        val players = listOf(viewer, teleporter)
        val harness = RootSyncHarness()
        val originalUpdateRunning = Server.updateRunning
        val originalItemManager = Server.itemManager

        try {
            Server.updateRunning = false
            Server.itemManager = testItemManager()

            harness.sync(players)
            drainOutbound(viewer)
            drainOutbound(teleporter)

            teleporter.transport(Position(3209, 3205, 0))
            teleporter.getNextPlayerMovement()

            val teleportCycle = harness.sync(players)
            val teleportPacket = drainSingleOutbound(viewer)
            assertEquals(1, teleportCycle.playerTeleportReinsertCount)
            assertEquals(1, readBits(teleportPacket.toByteArray(), 9, 1))
            assertEquals(3, readBits(teleportPacket.toByteArray(), 10, 2))
            teleportPacket.releaseAll()
            drainOutbound(teleporter)

            repeat(8) { step ->
                teleporter.addToWalkingQueue(teleporter.currentX + 1, teleporter.currentY)
                teleporter.getNextPlayerMovement()

                val cycle = harness.sync(players)
                val viewerPacket = drainSingleOutbound(viewer)
                val payload = viewerPacket.toByteArray()

                assertEquals(
                    0,
                    cycle.playerPacketsIdleTemplated,
                    "viewer fell back to idle template on post-teleport walking step ${step + 1}",
                )
                assertEquals(81, viewerPacket.opcode)
                assertEquals(0, readBits(payload, 0, 1))
                assertEquals(1, readBits(payload, 1, 8))
                assertEquals(1, readBits(payload, 9, 1))
                assertEquals(1, readBits(payload, 10, 2))
                assertEquals(0, cycle.playerTeleportReinsertCount)
                assertSame(teleporter, viewer.playerList[0])

                viewerPacket.releaseAll()
                drainOutbound(teleporter)
            }
        } finally {
            Server.updateRunning = originalUpdateRunning
            Server.itemManager = originalItemManager
            cleanupClient(viewer)
            cleanupClient(teleporter)
        }
    }

    private fun registerClient(slot: Int): Client =
        Client(EmbeddedChannel(), slot).apply {
            isActive = true
            loaded = true
            setPlayerName("post-teleport-$slot")
            moveTo(3200 + slot, 3200, 0)
            PlayerHandler.players[slot] = this
        }

    private fun cleanupClient(client: Client) {
        PlayerHandler.players[client.slot] = null
        drainOutbound(client)
        (client.channel as EmbeddedChannel).finishAndReleaseAll()
    }

    private fun drainOutbound(client: Client) {
        client.flushOutbound()
        val channel = client.channel as EmbeddedChannel
        while (true) {
            val outbound = channel.readOutbound<Any>() ?: break
            if (outbound is ByteMessage) {
                outbound.releaseAll()
            }
        }
    }

    private fun drainSingleOutbound(client: Client): ByteMessage {
        client.flushOutbound()
        val channel = client.channel as EmbeddedChannel
        val outbound = channel.readOutbound<Any>()
        assertNotNull(outbound)
        assertTrue(outbound is ByteMessage)
        assertNull(channel.readOutbound<Any>())
        return outbound as ByteMessage
    }

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

    private class RootSyncHarness {
        private val revisionIndex = PlayerSyncRevisionIndex()
        private val activityIndex = PlayerChunkActivityIndex()
        private var tick = 0L

        fun sync(players: List<Client>): SynchronizationCycle {
            tick++
            activityIndex.clear()
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
    }
}
