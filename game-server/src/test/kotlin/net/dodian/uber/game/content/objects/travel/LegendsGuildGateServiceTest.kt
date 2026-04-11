package net.dodian.uber.game.content.objects.travel

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.interaction.PersonalPassageService
import net.dodian.uber.game.systems.net.PacketWalkingService
import net.dodian.uber.game.systems.net.WalkRequest
import net.dodian.uber.game.systems.pathing.collision.CollisionManager
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LegendsGuildGateServiceTest {
    @BeforeEach
    fun setUp() {
        CollisionManager.global().clear()
        CollisionManager.global().flagSolid(2728, 3349, 0)
        CollisionManager.global().flagSolid(2729, 3349, 0)
    }

    @AfterEach
    fun tearDown() {
        CollisionManager.global().clear()
        PersonalPassageService.clearForTests()
        PlayerRegistry.playersOnline.clear()
    }

    @Test
    fun `premium player is force-routed through legends guild gate from south`() {
        val client = clientAt(slot = 1, nameKey = 11L, x = 2728, y = 3347)
        client.premium = true

        val allowed = LegendsGuildGateService.allowPassage(client)

        assertTrue(allowed)
        runMovementTicks(client, 8)
        assertEquals(2728, client.position.x)
        assertEquals(3350, client.position.y)
    }

    @Test
    fun `premium player is force-routed through legends guild gate from north`() {
        val client = clientAt(slot = 2, nameKey = 22L, x = 2729, y = 3351)
        client.premium = true

        val allowed = LegendsGuildGateService.allowPassage(client)

        assertTrue(allowed)
        runMovementTicks(client, 8)
        assertEquals(2729, client.position.x)
        assertEquals(3348, client.position.y)
    }

    @Test
    fun `non premium player is not moved through legends guild gate`() {
        val client = clientAt(slot = 3, nameKey = 33L, x = 2728, y = 3348)
        client.premium = false

        val allowed = LegendsGuildGateService.allowPassage(client)

        assertFalse(allowed)
        assertEquals(2728, client.position.x)
        assertEquals(3348, client.position.y)
    }

    @Test
    fun `visual open does not let other players pass without personal passage grant`() {
        val premium = clientAt(slot = 4, nameKey = 44L, x = 2728, y = 3347)
        premium.premium = true
        val blocked = clientAt(slot = 5, nameKey = 55L, x = 2729, y = 3348)
        blocked.premium = false

        assertTrue(LegendsGuildGateService.allowPassage(premium))

        walkOneTile(blocked, 2729, 3349)

        assertEquals(2729, blocked.position.x)
        assertEquals(3348, blocked.position.y)
    }

    private fun clientAt(slot: Int, nameKey: Long, x: Int, y: Int): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.longName = nameKey
        client.playerName = "gate-test-$slot"
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.dbId = nameKey.toInt()
        client.teleportTo(x, y, 0)
        primeMovementState(client)
        PlayerRegistry.playersOnline[nameKey] = client
        return client
    }

    private fun walkOneTile(client: Client, targetX: Int, targetY: Int) {
        primeMovementState(client)
        PacketWalkingService.handle(
            client,
            WalkRequest(
                opcode = 164,
                firstStepXAbs = targetX,
                firstStepYAbs = targetY,
                running = false,
                deltasX = intArrayOf(0),
                deltasY = intArrayOf(0),
            ),
        )
        client.postProcessing()
        client.getNextPlayerMovement()
    }

    private fun runMovementTicks(client: Client, ticks: Int) {
        repeat(ticks) {
            client.postProcessing()
            client.getNextPlayerMovement()
        }
    }

    private fun primeMovementState(player: Client) {
        player.getNextPlayerMovement()
        player.postProcessing()
        player.clearUpdateFlags()
    }
}
