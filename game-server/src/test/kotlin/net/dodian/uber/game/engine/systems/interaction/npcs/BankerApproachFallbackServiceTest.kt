package net.dodian.uber.game.engine.systems.interaction.npcs

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.systems.pathing.collision.CollisionManager
import net.dodian.uber.game.engine.systems.world.npc.NpcManager
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BankerApproachFallbackServiceTest {
    private var previousNpcManager: NpcManager? = null

    @BeforeEach
    fun setUp() {
        previousNpcManager = Server.npcManager
        Server.npcManager = previousNpcManager ?: NpcManager()
        Server.npcManager.npcMap.clear()
    }

    @AfterEach
    fun tearDown() {
        CollisionManager.global().clear()
        Server.npcManager = previousNpcManager
    }

    @Test
    fun `banker fallback routes player to customer side boundary tile`() {
        val client = clientAt(slot = 21, x = 2612, y = 3094, z = 0)
        val banker = Npc(300, 394, Position(2615, 3094, 0), 6)

        // Simulate the booth tile directly in front of the banker being blocked.
        CollisionManager.global().flagSolid(2614, 3094, 0)

        assertTrue(BankerApproachFallbackService.shouldAttemptFallback(client, banker, option = 1))
        assertTrue(BankerApproachFallbackService.tryRouteCustomerSide(client, banker))
        assertTrue(client.newWalkCmdSteps > 0)

        val last = client.newWalkCmdSteps - 1
        val baseX = client.mapRegionX * 8
        val baseY = client.mapRegionY * 8
        val destX = client.newWalkCmdX[last] + baseX
        val destY = client.newWalkCmdY[last] + baseY

        assertEquals(2616, destX)
        assertEquals(3094, destY)
    }

    @Test
    fun `banker fallback can still trigger while player has queued movement`() {
        val client = clientAt(slot = 22, x = 2612, y = 3094, z = 0)
        val banker = Npc(301, 394, Position(2615, 3094, 0), 6)
        client.newWalkCmdSteps = 2

        assertTrue(BankerApproachFallbackService.shouldAttemptFallback(client, banker, option = 1))
        assertFalse(BankerApproachFallbackService.shouldAttemptFallback(client, banker, option = 5))
    }

    private fun clientAt(slot: Int, x: Int, y: Int, z: Int): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.longName = slot.toLong()
        client.playerName = "banker-fallback-$slot"
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.validClient = true
        client.dbId = slot
        client.teleportTo(x, y, z)
        return client
    }
}
