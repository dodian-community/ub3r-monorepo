package net.dodian.uber.game.objects.travel

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.objects.DoorRegistry
import net.dodian.uber.game.engine.systems.interaction.PersonalPassageService
import net.dodian.uber.game.engine.systems.net.PacketWalkingService
import net.dodian.uber.game.engine.systems.net.WalkRequest
import net.dodian.uber.game.engine.systems.pathing.collision.CollisionManager
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LegendsGuildGateServiceTest {
    private lateinit var originalDoorId: IntArray
    private lateinit var originalDoorX: IntArray
    private lateinit var originalDoorY: IntArray
    private lateinit var originalDoorHeight: IntArray
    private lateinit var originalDoorFaceOpen: IntArray
    private lateinit var originalDoorFaceClosed: IntArray
    private lateinit var originalDoorFace: IntArray
    private lateinit var originalDoorState: IntArray

    @BeforeEach
    fun setUp() {
        originalDoorId = DoorRegistry.doorId.copyOf()
        originalDoorX = DoorRegistry.doorX.copyOf()
        originalDoorY = DoorRegistry.doorY.copyOf()
        originalDoorHeight = DoorRegistry.doorHeight.copyOf()
        originalDoorFaceOpen = DoorRegistry.doorFaceOpen.copyOf()
        originalDoorFaceClosed = DoorRegistry.doorFaceClosed.copyOf()
        originalDoorFace = DoorRegistry.doorFace.copyOf()
        originalDoorState = DoorRegistry.doorState.copyOf()
        CollisionManager.global().clear()
        CollisionManager.global().flagSolid(2728, 3349, 0)
        CollisionManager.global().flagSolid(2729, 3349, 0)
    }

    @AfterEach
    fun tearDown() {
        CollisionManager.global().clear()
        PersonalPassageService.clearForTests()
        LegendsGuildGateService.clearForTests()
        DoorRegistry.doorId = originalDoorId
        DoorRegistry.doorX = originalDoorX
        DoorRegistry.doorY = originalDoorY
        DoorRegistry.doorHeight = originalDoorHeight
        DoorRegistry.doorFaceOpen = originalDoorFaceOpen
        DoorRegistry.doorFaceClosed = originalDoorFaceClosed
        DoorRegistry.doorFace = originalDoorFace
        DoorRegistry.doorState = originalDoorState
        PlayerRegistry.playersOnline.clear()
    }

    @Test
    fun `premium player is force-routed through legends guild gate from south`() {
        val client = clientAt(slot = 1, nameKey = 11L, x = 2728, y = 3347)
        client.premium = true

        val allowed = LegendsGuildGateService.allowPassage(client)

        assertTrue(allowed)
        runMovementTicks(client, 10)
        assertEquals(2728, client.position.x)
        assertEquals(3350, client.position.y)
        assertEquals("success", LegendsGuildGateService.completionReasonForTests(client))
    }

    @Test
    fun `premium player already at entry tile still starts traversal and crosses`() {
        val client = clientAt(slot = 8, nameKey = 88L, x = 2728, y = 3348)
        client.premium = true

        val allowed = LegendsGuildGateService.allowPassage(client)

        assertTrue(allowed)
        runMovementTicks(client, 10)
        assertEquals(2728, client.position.x)
        assertEquals(3350, client.position.y)
        assertEquals("success", LegendsGuildGateService.completionReasonForTests(client))
    }

    @Test
    fun `premium player is force-routed through legends guild gate from north`() {
        val client = clientAt(slot = 2, nameKey = 22L, x = 2729, y = 3351)
        client.premium = true

        val allowed = LegendsGuildGateService.allowPassage(client)

        assertTrue(allowed)
        runMovementTicks(client, 10)
        assertEquals(2729, client.position.x)
        assertEquals(3348, client.position.y)
        assertEquals("success", LegendsGuildGateService.completionReasonForTests(client))
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

    @Test
    fun `stale queued walk is cleared before gate route to avoid backtrack`() {
        val premium = clientAt(slot = 9, nameKey = 99L, x = 2728, y = 3347)
        premium.premium = true
        queueWalkNoTick(premium, targetX = 2728, targetY = 3346)

        assertTrue(LegendsGuildGateService.allowPassage(premium))

        premium.postProcessing()
        premium.getNextPlayerMovement()
        LegendsGuildGateService.pumpTraversalForTests(premium)
        assertEquals(3347, premium.position.y)

        premium.postProcessing()
        premium.getNextPlayerMovement()
        LegendsGuildGateService.pumpTraversalForTests(premium)
        assertEquals(3348, premium.position.y)

        runMovementTicks(premium, 10)
        assertEquals(2728, premium.position.x)
        assertEquals(3350, premium.position.y)
    }

    @Test
    fun `visual snapshot falls back to built in legends gate faces when door registry rows are missing`() {
        DoorRegistry.doorId = IntArray(0)
        DoorRegistry.doorX = IntArray(0)
        DoorRegistry.doorY = IntArray(0)
        DoorRegistry.doorHeight = IntArray(0)
        DoorRegistry.doorFaceOpen = IntArray(0)
        DoorRegistry.doorFaceClosed = IntArray(0)
        DoorRegistry.doorFace = IntArray(0)
        DoorRegistry.doorState = IntArray(0)

        val premium = clientAt(slot = 6, nameKey = 66L, x = 2728, y = 3347)
        premium.premium = true

        assertTrue(LegendsGuildGateService.allowPassage(premium))
        val snapshot = LegendsGuildGateService.visualSnapshotForTests()
        assertEquals(0, snapshot?.left?.open)
        assertEquals(-3, snapshot?.left?.closed)
        assertEquals(-2, snapshot?.right?.open)
        assertEquals(-3, snapshot?.right?.closed)
    }

    @Test
    fun `visual snapshot uses door registry faces for close restore`() {
        DoorRegistry.doorId = intArrayOf(2391, 2392)
        DoorRegistry.doorX = intArrayOf(2728, 2729)
        DoorRegistry.doorY = intArrayOf(3349, 3349)
        DoorRegistry.doorHeight = intArrayOf(0, 0)
        DoorRegistry.doorFaceOpen = intArrayOf(0, 2)
        DoorRegistry.doorFaceClosed = intArrayOf(3, 1)
        DoorRegistry.doorFace = intArrayOf(0, 0)
        DoorRegistry.doorState = intArrayOf(0, 0)

        val premium = clientAt(slot = 7, nameKey = 77L, x = 2728, y = 3347)
        premium.premium = true

        assertTrue(LegendsGuildGateService.allowPassage(premium))
        val snapshot = LegendsGuildGateService.visualSnapshotForTests()
        assertEquals(3, snapshot?.left?.closed)
        assertEquals(1, snapshot?.right?.closed)
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
        queueWalkNoTick(client, targetX, targetY)
        client.postProcessing()
        client.getNextPlayerMovement()
    }

    private fun queueWalkNoTick(client: Client, targetX: Int, targetY: Int) {
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
    }

    private fun runMovementTicks(client: Client, ticks: Int) {
        repeat(ticks) {
            client.postProcessing()
            client.getNextPlayerMovement()
            LegendsGuildGateService.pumpTraversalForTests(client)
        }
    }

    private fun primeMovementState(player: Client) {
        player.getNextPlayerMovement()
        player.postProcessing()
        player.clearUpdateFlags()
    }
}

