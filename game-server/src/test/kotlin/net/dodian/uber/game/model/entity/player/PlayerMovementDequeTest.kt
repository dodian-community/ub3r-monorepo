package net.dodian.uber.game.model.entity.player

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.engine.routing.WorldRouteService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlayerMovementDequeTest {
    @BeforeEach
    fun setUp() {
        WorldRouteService.clear()
        WorldRouteService.allocateZone(3200, 3200, 0)
    }

    @AfterEach
    fun tearDown() = WorldRouteService.clear()

    @Test
    fun `walk consumes one validated tile per cycle and polls waypoint only when reached`() {
        val player = initializedPlayer()
        player.buttonOnRun = false
        player.replaceMovementRoute(listOf(MovementPoint(3202, 3200, 0)), false)

        player.getNextPlayerMovement()
        assertEquals(3201, player.position.x)
        assertTrue(player.hasMovementRoute())

        player.getNextPlayerMovement()
        assertEquals(3202, player.position.x)
        assertFalse(player.hasMovementRoute())
    }

    @Test
    fun `running consumes two validated tiles in one cycle`() {
        val player = initializedPlayer()
        player.buttonOnRun = true
        player.replaceMovementRoute(listOf(MovementPoint(3202, 3200, 0)), true)

        player.getNextPlayerMovement()

        assertEquals(3202, player.position.x)
        assertTrue(player.primaryDirection >= 0)
        assertTrue(player.secondaryDirection >= 0)
        assertFalse(player.hasMovementRoute())
    }

    @Test
    fun `blocked and different-plane routes cancel cleanly`() {
        val blocked = initializedPlayer()
        blocked.buttonOnRun = false
        WorldRouteService.markTerrainBlocked(3201, 3200, 0)
        blocked.replaceMovementRoute(listOf(MovementPoint(3202, 3200, 0)), false)

        blocked.getNextPlayerMovement()

        assertEquals(3200, blocked.position.x)
        assertFalse(blocked.hasMovementRoute())

        val wrongPlane = initializedPlayer()
        wrongPlane.replaceMovementRoute(listOf(MovementPoint(3201, 3200, 1)), false)
        wrongPlane.getNextPlayerMovement()
        assertFalse(wrongPlane.hasMovementRoute())
    }

    @Test
    fun `teleport clears queued movement`() {
        val player = initializedPlayer()
        player.replaceMovementRoute(listOf(MovementPoint(3205, 3200, 0)), false)
        player.teleportToX = 3210
        player.teleportToY = 3210
        player.teleportToZ = 0

        player.getNextPlayerMovement()

        assertEquals(3210, player.position.x)
        assertEquals(3210, player.position.y)
        assertFalse(player.hasMovementRoute())
    }

    @Test
    fun `newer route replaces older destination without stale movement`() {
        val player = initializedPlayer()
        player.buttonOnRun = false
        player.replaceMovementRoute(listOf(MovementPoint(3205, 3200, 0)), false)
        player.replaceMovementRoute(listOf(MovementPoint(3200, 3202, 0)), false)

        player.getNextPlayerMovement()

        assertEquals(3200, player.position.x)
        assertEquals(3201, player.position.y)
        assertEquals(MovementPoint(3200, 3202, 0), player.movementDestination())
    }

    private fun initializedPlayer(): Client =
        Client(EmbeddedChannel(), 1).also { player ->
            player.teleportToX = 3200
            player.teleportToY = 3200
            player.teleportToZ = 0
            player.getNextPlayerMovement()
        }
}
