package net.dodian.uber.game.content.skills.agility

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.content.skills.agility.runtime.AgilityTraversalService
import net.dodian.uber.game.content.skills.runtime.SkillActionContext
import net.dodian.uber.game.content.skills.runtime.SkillTraversalMovement
import net.dodian.uber.game.content.skills.runtime.SkillTraversalPlan
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.follow.FollowService
import net.dodian.uber.game.systems.interaction.PersonalPassageService
import net.dodian.uber.game.systems.pathing.collision.CollisionManager
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AgilityTraversalServiceTest {
    @AfterEach
    fun tearDown() {
        FollowService.clear()
        PlayerRegistry.playersOnline.clear()
        CollisionManager.global().clear()
        PersonalPassageService.clearForTests()
    }

    @Test
    fun `gnome log traversal crosses blocked segment with personal passage edges`() {
        val client = clientAt(slot = 1, nameKey = 2001L, x = 2474, y = 3436, z = 0)
        CollisionManager.global().flagSolid(2474, 3430, 0)

        val started = Agility(client).GnomeLog()

        assertTrue(started)
        runMovementTicks(client, ticks = 20)
        assertEquals(2474, client.position.x)
        assertEquals(3429, client.position.y)
    }

    @Test
    fun `gnome balancing rope traversal crosses blocked segment with personal passage edges`() {
        val client = clientAt(slot = 2, nameKey = 2002L, x = 2477, y = 3420, z = 2)
        CollisionManager.global().flagSolid(2478, 3420, 2)

        val started = Agility(client).GnomeRope()

        assertTrue(started)
        runMovementTicks(client, ticks = 20)
        assertEquals(2483, client.position.x)
        assertEquals(3420, client.position.y)
        assertEquals(2, client.position.z)
    }

    @Test
    fun `canceling traversal clears movement lock and personal passage grants`() {
        val client = clientAt(slot = 3, nameKey = 2003L, x = 3200, y = 3200, z = 0)
        val context =
            SkillActionContext(
                player = client,
                objectId = 9999,
                option = 1,
                objectPosition = Position(3201, 3200, 0),
            )
        val plan =
            SkillTraversalPlan(
                name = "agility.test.cancel",
                movement = SkillTraversalMovement(deltaX = 1, deltaY = 0, durationMs = 4_800, movementAnimationId = 762),
                passageEdges = {
                    listOf(
                        Position(3200, 3200, 0) to Position(3201, 3200, 0),
                    )
                },
            )

        val started = AgilityTraversalService.execute(context, plan)

        assertTrue(started)
        assertTrue(client.UsingAgility)
        assertTrue(PersonalPassageService.canTraverse(client, 3200, 3200, 3201, 3200, 0))

        AgilityTraversalService.cancel(context, plan)

        assertFalse(client.UsingAgility)
        assertFalse(PersonalPassageService.canTraverse(client, 3200, 3200, 3201, 3200, 0))
    }

    private fun clientAt(
        slot: Int,
        nameKey: Long,
        x: Int,
        y: Int,
        z: Int,
    ): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.longName = nameKey
        client.playerName = "agility-$slot"
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.dbId = nameKey.toInt()
        client.teleportTo(x, y, z)
        primeMovementState(client)
        PlayerRegistry.playersOnline[nameKey] = client
        return client
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
