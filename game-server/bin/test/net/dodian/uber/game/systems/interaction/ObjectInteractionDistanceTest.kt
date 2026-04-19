package net.dodian.uber.game.systems.interaction

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ObjectInteractionDistanceTest {
    @Test
    fun `policy nearest boundary cardinal accepts player standing on clicked tile`() {
        val player = Client(EmbeddedChannel(), 1)
        player.moveTo(2604, 3100, 0)
        val walkTo = Position(2604, 3100, 0)

        val resolved =
            ObjectInteractionDistance.resolveDistancePosition(
                client = player,
                walkTo = walkTo,
                objectId = 20885,
                objectData = null,
                def = null,
                mode = ObjectInteractionDistance.DistanceMode.POLICY_NEAREST_BOUNDARY_CARDINAL,
            )

        assertNotNull(resolved)
        assertEquals(walkTo, resolved)
    }

    @Test
    fun `policy nearest boundary cardinal accepts adjacent clicked tile`() {
        val player = Client(EmbeddedChannel(), 1)
        player.moveTo(2604, 3099, 0)
        val walkTo = Position(2604, 3100, 0)

        val resolved =
            ObjectInteractionDistance.resolveDistancePosition(
                client = player,
                walkTo = walkTo,
                objectId = 20885,
                objectData = null,
                def = null,
                mode = ObjectInteractionDistance.DistanceMode.POLICY_NEAREST_BOUNDARY_CARDINAL,
            )

        assertNotNull(resolved)
        assertEquals(walkTo, resolved)
    }
}
