package net.dodian.uber.game.content.objects.impl.travel

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.content.objects.ObjectContentRegistry
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.interaction.ObjectInteractionPolicy
import net.dodian.uber.game.runtime.loop.GameThreadTimers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VerticalTravelObjectsTest {
    @AfterEach
    fun cleanupTimers() {
        GameThreadTimers.clearForTests()
        ObjectContentRegistry.resetForTests()
    }

    @Test
    fun `ladder first click requires settled movement`() {
        val policy =
            LadderObjects.clickInteractionPolicy(
                option = 1,
                objectId = 1747,
                position = Position(3200, 3200, 0),
                obj = null,
            )

        assertNotNull(policy)
        assertEquals(ObjectInteractionPolicy.DistanceRule.LEGACY_OBJECT_DISTANCE, policy!!.distanceRule)
        assertTrue(policy.requireMovementSettled)
        assertEquals(1, policy.settleTicks)
    }

    @Test
    fun `vertical travel stages teleport without mutating position immediately`() {
        val client = Client(EmbeddedChannel(), 1)
        client.moveTo(3000, 3000, 0)

        val started =
            VerticalTravel.start(
                client,
                VerticalTravelCompletion.QueuedDestination(Position(3000, 3001, 1)),
                VerticalTravelStyle(animationId = -1, delayMs = 0L),
            )

        assertTrue(started)
        assertEquals(3000, client.position.x)
        assertEquals(3000, client.position.y)
        assertEquals(0, client.position.z)

        GameThreadTimers.drainDue()

        assertEquals(3000, client.position.x)
        assertEquals(3000, client.position.y)
        assertEquals(0, client.position.z)
        assertEquals(3000, client.teleportToX)
        assertEquals(3001, client.teleportToY)
        assertEquals(1, client.teleportToZ)
        (client.channel as EmbeddedChannel).finishAndReleaseAll()
    }

    @Test
    fun `legends up no longer arms legacy stairs`() {
        val client = Client(EmbeddedChannel(), 1)
        client.premium = true
        client.moveTo(2732, 3377, 0)

        val handled = StaircaseObjects.onFirstClick(client, 1725, Position(2732, 3377, 0), null)

        assertTrue(handled)
        assertEquals(0, client.stairs)
        assertTrue(client.isVerticalTransitionActive())
        (client.channel as EmbeddedChannel).finishAndReleaseAll()
    }

    @Test
    fun `vertical teleport objects are no longer registered in generic teleport module`() {
        assertFalse(TeleportObjects.objectIds.contains(16683))
        assertFalse(TeleportObjects.objectIds.contains(17122))
        assertFalse(TeleportObjects.objectIds.contains(2796))
    }

    @Test
    fun `registry boots migrated vertical ids without overlap`() {
        assertDoesNotThrow { ObjectContentRegistry.bootstrap() }
        assertEquals(1, ObjectContentRegistry.bindingsForObjectForTests(16683).size)
        assertEquals(1, ObjectContentRegistry.bindingsForObjectForTests(17122).size)
        assertEquals(1, ObjectContentRegistry.bindingsForObjectForTests(1725).size)
    }

    @Test
    fun `clear vertical travel state resets legacy stair residue`() {
        val client = Client(EmbeddedChannel(), 1)
        client.moveTo(2732, 3377, 0)
        client.stairs = 22
        client.skillX = 2732
        client.skillY = 3377
        client.stairDistance = 2
        client.stairDistanceAdd = 1
        setIsStair(client, true)
        client.beginVerticalTransition(250L)

        client.clearVerticalTravelState()

        assertFalse(client.isVerticalTransitionActive())
        assertEquals(0, client.stairs)
        assertEquals(-1, client.skillX)
        assertEquals(-1, client.skillY)
        assertEquals(1, client.stairDistance)
        assertEquals(0, client.stairDistanceAdd)
        assertFalse(getIsStair(client))
        (client.channel as EmbeddedChannel).finishAndReleaseAll()
    }

    private fun setIsStair(client: Client, value: Boolean) {
        val field = client.javaClass.superclass.getDeclaredField("IsStair")
        field.isAccessible = true
        field.setBoolean(client, value)
    }

    private fun getIsStair(client: Client): Boolean {
        val field = client.javaClass.superclass.getDeclaredField("IsStair")
        field.isAccessible = true
        return field.getBoolean(client)
    }
}
