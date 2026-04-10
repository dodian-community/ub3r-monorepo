package net.dodian.uber.game.content.objects.travel

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.interaction.PersonalObjectService
import net.dodian.uber.game.systems.interaction.PersonalPassageService
import net.dodian.uber.game.systems.net.PacketWalkingService
import net.dodian.uber.game.systems.net.WalkRequest
import net.dodian.uber.game.systems.pathing.collision.CollisionManager
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
        PersonalObjectService.clearForTests()
    }

    @Test
    fun `premium player receives temporary gate passage and can walk north through legends guild gate`() {
        val client = clientAt(2728, 3348)
        client.premium = true

        val allowed = LegendsGuildGateService.allowPassage(client)

        assertTrue(allowed)
        assertEquals(2728, client.position.x)
        assertEquals(3348, client.position.y)

        walkOneTile(client, 2728, 3349)
        assertEquals(2728, client.position.x)
        assertEquals(3349, client.position.y)

        walkOneTile(client, 2728, 3350)
        assertEquals(2728, client.position.x)
        assertEquals(3350, client.position.y)
    }

    @Test
    fun `premium player receives temporary gate passage and can walk south through legends guild gate`() {
        val client = clientAt(2729, 3350)
        client.premium = true

        val allowed = LegendsGuildGateService.allowPassage(client)

        assertTrue(allowed)
        assertEquals(2729, client.position.x)
        assertEquals(3350, client.position.y)

        walkOneTile(client, 2729, 3349)
        assertEquals(2729, client.position.x)
        assertEquals(3349, client.position.y)

        walkOneTile(client, 2729, 3348)
        assertEquals(2729, client.position.x)
        assertEquals(3348, client.position.y)
    }

    @Test
    fun `non premium player is not moved through legends guild gate`() {
        val client = clientAt(2728, 3348)
        client.premium = false

        val allowed = LegendsGuildGateService.allowPassage(client)

        assertFalse(allowed)
        assertEquals(2728, client.position.x)
        assertEquals(3348, client.position.y)
    }

    @Test
    fun `without temporary gate passage movement remains blocked`() {
        val client = clientAt(2728, 3348)

        walkOneTile(client, 2728, 3349)

        assertEquals(2728, client.position.x)
        assertEquals(3348, client.position.y)
    }

    private fun clientAt(x: Int, y: Int): Client {
        val client = Client(EmbeddedChannel(), 1)
        client.longName = 1L
        client.playerName = "gate-test"
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.dbId = 1
        client.teleportTo(x, y, 0)
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

    private fun primeMovementState(player: Client) {
        player.getNextPlayerMovement()
        player.postProcessing()
        player.clearUpdateFlags()
    }
}

