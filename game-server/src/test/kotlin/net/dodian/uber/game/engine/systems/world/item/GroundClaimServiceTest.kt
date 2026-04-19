package net.dodian.uber.game.engine.systems.world.item

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GroundItem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroundClaimServiceTest {
    @AfterEach
    fun tearDown() {
        Ground.ground_items.clear()
        Ground.untradeable_items.clear()
        Ground.tradeable_items.clear()
    }

    @Test
    fun `only one claimant can reserve same ground item`() {
        val item = GroundItem(Position(3200, 3200, 0), 995, 1, 50, true)
        Ground.addItem(item)
        val first = testClient(7001)
        val second = testClient(7002)

        assertTrue(Ground.tryClaimPickup(first, item))
        assertFalse(Ground.tryClaimPickup(second, item))
    }

    @Test
    fun `released claim allows another claimant`() {
        val item = GroundItem(Position(3200, 3200, 0), 995, 1, 50, true)
        Ground.addItem(item)
        val first = testClient(7003)
        val second = testClient(7004)

        assertTrue(Ground.tryClaimPickup(first, item))
        Ground.releaseClaim(item)
        assertTrue(Ground.tryClaimPickup(second, item))
    }

    private fun testClient(slot: Int): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.longName = slot.toLong()
        client.playerName = "ground-claim-$slot"
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.dbId = slot
        client.teleportTo(3200, 3200, 0)
        return client
    }
}
