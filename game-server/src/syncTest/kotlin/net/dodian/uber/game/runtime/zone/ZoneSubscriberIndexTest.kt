package net.dodian.uber.game.runtime.zone

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.chunk.ChunkManager
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ZoneSubscriberIndexTest {

    @AfterEach
    fun tearDown() {
        Server.chunkManager = null
    }

    @Test
    fun `viewer lookup uses candidate chunks before exact filtering`() {
        Server.chunkManager = ChunkManager()
        val near = client(1, 3200, 3200)
        val alsoNear = client(2, 3208, 3208)
        val far = client(3, 3320, 3320)

        val delta = object : ZoneDelta() {
            override fun appliesTo(viewer: Client): Boolean =
                viewer.position.z == 0 &&
                    kotlin.math.abs(viewer.position.x - 3200) <= 16 &&
                    kotlin.math.abs(viewer.position.y - 3200) <= 16

            override fun deliver(viewer: Client) = Unit

            override fun candidateChunkKeys(): LongArray = longArrayOf(packChunkKey(394, 394), packChunkKey(395, 395))
        }

        val viewers = ZoneSubscriberIndex().viewersFor(delta, listOf(near, alsoNear, far))

        assertEquals(2, viewers.size)
        assertTrue(viewers.contains(near))
        assertTrue(viewers.contains(alsoNear))
    }

    private fun client(slot: Int, x: Int, y: Int): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.isActive = true
        client.loaded = true
        client.moveTo(x, y, 0)
        client.syncChunkMembership()
        return client
    }

    private fun packChunkKey(chunkX: Int, chunkY: Int): Long = (chunkX.toLong() shl 32) xor (chunkY.toLong() and 0xffffffffL)
}
