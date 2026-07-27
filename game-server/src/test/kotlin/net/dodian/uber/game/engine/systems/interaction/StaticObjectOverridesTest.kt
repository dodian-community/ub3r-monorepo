package net.dodian.uber.game.engine.systems.interaction

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.codec.ByteMessage
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

private const val SET_MAP_OPCODE = 85
private const val TEST_DISTANCE = 40 // beyond the old 32-tile radius, within the fixed 52 and ReplaceObject2's own 60-tile gate

class StaticObjectOverridesTest {
    @Test
    fun `replayTo reaches an override 40 tiles away, beyond the old 32-tile radius`() {
        // Don't assume any single hardcoded/TOML-loaded override is isolated - the real override
        // list (`StaticObjectOverrides.all()`) can be dense in places. Instead, find one override
        // with no OTHER override within 60 tiles (ReplaceObject2's own gate), so a viewer placed
        // TEST_DISTANCE away is testing exactly this one override's replay, not incidental noise.
        val overrides = StaticObjectOverrides.all()
        val isolated = overrides.first { candidate ->
            overrides.none { other ->
                other !== candidate &&
                    other.position.z == candidate.position.z &&
                    abs(other.position.x - candidate.position.x) <= 60 &&
                    abs(other.position.y - candidate.position.y) <= 60
            }
        }

        val viewer = Client(EmbeddedChannel(), 1).apply {
            moveTo(isolated.position.x, isolated.position.y - TEST_DISTANCE, isolated.position.z)
        }

        StaticObjectOverrides.replayTo(viewer)

        val message = (viewer.channel as EmbeddedChannel).readOutbound<ByteMessage>()
        assertNotNull(message, "an override $TEST_DISTANCE tiles away must be replayed on region entry")
        assertTrue(message!!.opcode == SET_MAP_OPCODE, "first packet ReplaceObject2 sends is always SetMap")
    }
}
