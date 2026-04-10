package net.dodian.uber.game.systems.follow

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.net.PacketWalkingService
import net.dodian.uber.game.systems.net.WalkRequest
import net.dodian.uber.game.systems.pathing.collision.CollisionDirection
import net.dodian.uber.game.systems.pathing.collision.CollisionManager
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FollowServiceTest {

    @AfterEach
    fun tearDown() {
        FollowService.clear()
        PlayerRegistry.playersOnline.clear()
        CollisionManager.global().clear()
    }

    @Test
    fun `adjacent follower faces target and does not enqueue movement`() {
        val follower = testClient(slot = 1, nameKey = 1001L, x = 3200, y = 3200)
        val leader = testClient(slot = 2, nameKey = 1002L, x = 3201, y = 3200)

        FollowService.processFollowing(follower, leader)

        assertFacingPlayer(follower, leader.slot)
        assertEquals(0, follower.newWalkCmdSteps)
    }

    @Test
    fun `follow request stores state until the tick processor applies the mask`() {
        val follower = testClient(slot = 3, nameKey = 1003L, x = 3210, y = 3210)
        val leader = testClient(slot = 4, nameKey = 1004L, x = 3215, y = 3210)

        FollowService.requestFollow(follower, leader)

        assertTrue(FollowService.isFollowing(follower))
        assertEquals(-1, follower.getFaceTarget())
        assertFalse(follower.wQueueReadPtr != follower.wQueueWritePtr)

        FollowService.processTick()

        assertFacingPlayer(follower, leader.slot)
        assertTrue(FollowService.isFollowing(follower))
    }

    @Test
    fun `follow routes around blocked tile using canonical walk command buffers`() {
        val follower = testClient(slot = 5, nameKey = 1005L, x = 3200, y = 3200)
        val leader = testClient(slot = 6, nameKey = 1006L, x = 3202, y = 3200)
        CollisionManager.global().flagSolid(3201, 3200, 0)

        FollowService.processFollowing(follower, leader)

        assertFacingPlayer(follower, leader.slot)
        assertTrue(follower.newWalkCmdSteps > 0)
        assertTrue(follower.newWalkCmdIsRunning)

        val baseX = follower.mapRegionX * 8
        val baseY = follower.mapRegionY * 8
        val waypoints =
            (0 until follower.newWalkCmdSteps)
                .map { (follower.newWalkCmdX[it] + baseX) to (follower.newWalkCmdY[it] + baseY) }

        assertFalse(waypoints.contains(3201 to 3200))
        val destination = waypoints.last()
        assertTrue(kotlin.math.abs(destination.first - leader.position.x) <= 1)
        assertTrue(kotlin.math.abs(destination.second - leader.position.y) <= 1)
    }

    @Test
    fun `follow clears face target and state when target becomes invalid`() {
        val follower = testClient(slot = 7, nameKey = 1007L, x = 3200, y = 3200)
        val leader = testClient(slot = 8, nameKey = 1008L, x = 3202, y = 3200)

        FollowService.requestFollow(follower, leader)
        FollowService.processTick()

        assertTrue(FollowService.isFollowing(follower))
        assertFacingPlayer(follower, leader.slot)

        leader.disconnected = true
        FollowService.processTick()

        assertFalse(FollowService.isFollowing(follower))
        assertEquals(-1, follower.getFaceTarget())
        assertTrue(follower.updateFlags.isRequired(UpdateFlag.FACE_CHARACTER))
        assertEquals(0, follower.newWalkCmdSteps)
    }

    @Test
    fun `follow targets tile behind leader last movement direction`() {
        val follower = testClient(slot = 11, nameKey = 1011L, x = 3200, y = 3200)
        val leader = testClient(slot = 12, nameKey = 1012L, x = 3202, y = 3200)
        leader.setLastWalkDelta(1, 0)

        FollowService.processFollowing(follower, leader)

        assertTrue(follower.newWalkCmdSteps > 0)
        val baseX = follower.mapRegionX * 8
        val baseY = follower.mapRegionY * 8
        val destinationX = follower.newWalkCmdX[follower.newWalkCmdSteps - 1] + baseX
        val destinationY = follower.newWalkCmdY[follower.newWalkCmdSteps - 1] + baseY
        assertEquals(3201, destinationX)
        assertEquals(3200, destinationY)
    }

    @Test
    fun `target unchanged does not enqueue new follow route commands`() {
        val follower = testClient(slot = 13, nameKey = 1013L, x = 3201, y = 3200)
        val leader = testClient(slot = 14, nameKey = 1014L, x = 3203, y = 3200)
        leader.setLastWalkDelta(1, 0)

        FollowService.processFollowing(follower, leader)

        val baseX = follower.mapRegionX * 8
        val baseY = follower.mapRegionY * 8
        val firstDestinationX = follower.newWalkCmdX[follower.newWalkCmdSteps - 1] + baseX
        val firstDestinationY = follower.newWalkCmdY[follower.newWalkCmdSteps - 1] + baseY
        assertEquals(3202, firstDestinationX)
        assertEquals(3200, firstDestinationY)

        // Simulate queued movement still pending so repath is driven by direction change.
        follower.wQueueReadPtr = 0
        follower.wQueueWritePtr = 1
        follower.newWalkCmdSteps = 0
        leader.setLastWalkDelta(0, 1)

        FollowService.processFollowing(follower, leader)

        assertEquals(0, follower.newWalkCmdSteps)
    }

    @Test
    fun `stationary leader keeps last non-zero direction for walk-behind destination`() {
        val follower = testClient(slot = 17, nameKey = 1017L, x = 3201, y = 3200)
        val leader = testClient(slot = 18, nameKey = 1018L, x = 3203, y = 3200)
        leader.setLastWalkDelta(1, 0)

        FollowService.processFollowing(follower, leader)

        val baseX = follower.mapRegionX * 8
        val baseY = follower.mapRegionY * 8
        val firstDestinationX = follower.newWalkCmdX[follower.newWalkCmdSteps - 1] + baseX
        val firstDestinationY = follower.newWalkCmdY[follower.newWalkCmdSteps - 1] + baseY
        assertEquals(3202, firstDestinationX)
        assertEquals(3200, firstDestinationY)

        follower.wQueueReadPtr = 0
        follower.wQueueWritePtr = 1
        follower.newWalkCmdSteps = 0
        leader.setLastWalkDelta(0, 0)

        FollowService.processFollowing(follower, leader)

        assertEquals(0, follower.newWalkCmdSteps)
    }

    @Test
    fun `stationary adjacent target does not jitter with repeated follow ticks`() {
        val follower = testClient(slot = 21, nameKey = 1021L, x = 3200, y = 3200)
        val leader = testClient(slot = 22, nameKey = 1022L, x = 3201, y = 3200)

        FollowService.requestFollow(follower, leader)
        FollowService.processTick()
        assertFacingPlayer(follower, leader.slot)
        assertEquals(0, follower.newWalkCmdSteps)

        // Simulate movement queue consumed; follow should hold interaction without requeueing.
        follower.wQueueReadPtr = 0
        follower.wQueueWritePtr = 0
        follower.newWalkCmdSteps = 0

        FollowService.processTick()
        assertFacingPlayer(follower, leader.slot)
        assertEquals(0, follower.newWalkCmdSteps)
    }

    @Test
    fun `stationary far target does not continuously repath when unchanged`() {
        val follower = testClient(slot = 23, nameKey = 1023L, x = 3200, y = 3200)
        val leader = testClient(slot = 24, nameKey = 1024L, x = 3205, y = 3200)

        FollowService.requestFollow(follower, leader)
        FollowService.processTick()
        assertTrue(follower.newWalkCmdSteps > 0)

        follower.wQueueReadPtr = 0
        follower.wQueueWritePtr = 0
        follower.newWalkCmdSteps = 0

        FollowService.processTick()
        assertEquals(0, follower.newWalkCmdSteps)
    }

    @Test
    fun `mutual follow with unchanged targets does not force dance movement`() {
        val a = testClient(slot = 25, nameKey = 1025L, x = 3300, y = 3300)
        val b = testClient(slot = 26, nameKey = 1026L, x = 3301, y = 3300)

        FollowService.requestFollow(a, b)
        FollowService.requestFollow(b, a)

        FollowService.processTick()
        assertEquals(0, a.newWalkCmdSteps)
        assertEquals(0, b.newWalkCmdSteps)
        assertFacingPlayer(a, b.slot)
        assertFacingPlayer(b, a.slot)
    }

    @Test
    fun `follow avoids corner-clipped adjacent tile when behind tile is wall-blocked`() {
        val follower = testClient(slot = 19, nameKey = 1019L, x = 3200, y = 3202)
        val leader = testClient(slot = 20, nameKey = 1020L, x = 3202, y = 3202)
        leader.setLastWalkDelta(1, 0)

        CollisionManager.global().wall(3201, 3202, 0, CollisionDirection.EAST)

        FollowService.processFollowing(follower, leader)

        assertTrue(follower.newWalkCmdSteps > 0)
        val baseX = follower.mapRegionX * 8
        val baseY = follower.mapRegionY * 8
        val destinationX = follower.newWalkCmdX[follower.newWalkCmdSteps - 1] + baseX
        val destinationY = follower.newWalkCmdY[follower.newWalkCmdSteps - 1] + baseY

        assertFalse(destinationX == 3201 && destinationY == 3202)
        assertTrue(kotlin.math.abs(destinationX - leader.position.x) <= 1)
        assertTrue(kotlin.math.abs(destinationY - leader.position.y) <= 1)

        if (destinationX != leader.position.x || destinationY != leader.position.y) {
            val stepX = leader.position.x - destinationX
            val stepY = leader.position.y - destinationY
            assertTrue(CollisionManager.global().traversable(leader.position.x, leader.position.y, 0, stepX, stepY))
        }
    }

    @Test
    fun `overlapping follower takes a random cardinal step`() {
        val follower = testClient(slot = 9, nameKey = 1009L, x = 3200, y = 3200)
        val leader = testClient(slot = 10, nameKey = 1010L, x = 3200, y = 3200)

        FollowService.processFollowing(follower, leader)

        assertFacingPlayer(follower, leader.slot)
        assertEquals(1, follower.newWalkCmdSteps)

        val baseX = follower.mapRegionX * 8
        val baseY = follower.mapRegionY * 8
        val stepX = follower.newWalkCmdX[0] + baseX
        val stepY = follower.newWalkCmdY[0] + baseY
        val dx = kotlin.math.abs(stepX - follower.position.x)
        val dy = kotlin.math.abs(stepY - follower.position.y)
        assertTrue(dx + dy == 1)
    }

    @Test
    fun `tile click walk cancels follow intent and preserves queued movement`() {
        assertManualWalkCancelsFollowAndKeepsRoute(opcode = 164)
    }

    @Test
    fun `minimap click walk cancels follow intent and preserves queued movement`() {
        assertManualWalkCancelsFollowAndKeepsRoute(opcode = 248)
    }

    private fun assertManualWalkCancelsFollowAndKeepsRoute(opcode: Int) {
        val follower = testClient(slot = 15, nameKey = 1015L, x = 3200, y = 3200)
        val leader = testClient(slot = 16, nameKey = 1016L, x = 3202, y = 3200)

        FollowService.requestFollow(follower, leader)
        FollowService.processTick()
        assertTrue(FollowService.isFollowing(follower))
        assertFacingPlayer(follower, leader.slot)

        PacketWalkingService.handle(
            follower,
            WalkRequest(
                opcode = opcode,
                firstStepXAbs = 3201,
                firstStepYAbs = 3200,
                running = false,
                deltasX = intArrayOf(0),
                deltasY = intArrayOf(0),
            ),
        )

        assertFalse(FollowService.isFollowing(follower))
        assertEquals(65535, follower.getFaceTarget())
        assertEquals(1, follower.newWalkCmdSteps)
        val baseX = follower.mapRegionX * 8
        val baseY = follower.mapRegionY * 8
        assertEquals(3201, follower.newWalkCmdX[0] + baseX)
        assertEquals(3200, follower.newWalkCmdY[0] + baseY)
    }

    @Test
    fun `accepted walking step does not persist face replay state`() {
        val walker = testClient(slot = 29, nameKey = 1029L, x = 3200, y = 3200)

        primeMovementState(walker)
        PacketWalkingService.handle(
            walker,
            WalkRequest(
                opcode = 164,
                firstStepXAbs = 3201,
                firstStepYAbs = 3200,
                running = false,
                deltasX = intArrayOf(0),
                deltasY = intArrayOf(0),
            ),
        )

        runMovementTick(walker)

        assertEquals(0, walker.persistedFaceX)
        assertEquals(0, walker.persistedFaceY)
    }

    private fun testClient(
        slot: Int,
        nameKey: Long,
        x: Int,
        y: Int,
    ): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.longName = nameKey
        client.playerName = "player-$slot"
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.dbId = nameKey.toInt()
        client.teleportTo(x, y, 0)
        PlayerRegistry.playersOnline[nameKey] = client
        return client
    }

    private fun runMovementTick(vararg players: Client) {
        players.sortedBy { it.slot }.forEach { player ->
            player.postProcessing()
            player.getNextPlayerMovement()
        }
    }

    private fun primeMovementState(player: Client) {
        player.getNextPlayerMovement()
        player.postProcessing()
        player.clearUpdateFlags()
    }

    private fun assertFacingPlayer(player: Client, targetSlot: Int) {
        assertEquals(32768 + targetSlot, player.faceTarget)
        assertTrue(player.updateFlags.isRequired(UpdateFlag.FACE_CHARACTER))
        assertFalse(player.updateFlags.isRequired(UpdateFlag.FACE_COORDINATE))
    }
}
