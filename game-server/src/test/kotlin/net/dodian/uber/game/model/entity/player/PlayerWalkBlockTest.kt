package net.dodian.uber.game.model.entity.player

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.engine.loop.GameCycleClock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlayerWalkBlockTest {
    @BeforeEach
    fun setUp() {
        GameCycleClock.syncTo(100)
    }

    @AfterEach
    fun tearDown() {
        GameCycleClock.syncTo(0)
    }

    @Test
    fun `applyWalkBlockMs blocks movement for exactly the requested number of ticks`() {
        val client = Client(EmbeddedChannel(), 1)

        client.applyWalkBlockMs(600) // 1 tick

        assertTrue(client.isWalkBlocked())
        GameCycleClock.advance()
        assertFalse(client.isWalkBlocked())
    }

    @Test
    fun `applyWalkBlockMs never shortens an existing longer block`() {
        val client = Client(EmbeddedChannel(), 1)

        client.applyWalkBlockMs(1800) // 3 ticks
        client.applyWalkBlockMs(600) // must not shorten the block already in effect

        GameCycleClock.advance()
        GameCycleClock.advance()
        assertTrue(client.isWalkBlocked(), "should still be blocked 2 cycles into a 3-cycle block")
        GameCycleClock.advance()
        assertFalse(client.isWalkBlocked())
    }

    @Test
    fun `a zero or negative duration does not apply a block`() {
        val client = Client(EmbeddedChannel(), 1)

        client.applyWalkBlockMs(0)
        client.applyWalkBlockMs(-100)

        assertFalse(client.isWalkBlocked())
    }
}
