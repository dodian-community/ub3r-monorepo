package net.dodian.uber.game.engine.sync.player

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerVisibilityRulesTest {
    private val clients = mutableListOf<Client>()

    @AfterEach
    fun tearDown() {
        clients.forEach {
            it.saveNeeded = false
            it.destruct()
        }
        clients.clear()
    }

    @Test
    fun `a candidate exactly 16 tiles away on the positive x axis is not visible`() {
        // writeStagedLocalAdd encodes add-local deltas as a wrap-add 5-bit field: a negative
        // delta gets +32 added, then the client re-signs raw values above 15 back to negative.
        // A delta of exactly +16 is therefore indistinguishable on the wire from -16 - it must be
        // excluded from visibility, matching the (-16, 15) bound already used for NPCs.
        val viewer = client(1, "viewer", 3200, 3200)
        val unrepresentable = client(2, "unrepresentable", 3216, 3200)
        val maxPositive = client(3, "max-positive", 3215, 3200)
        val maxNegative = client(4, "max-negative", 3184, 3200)

        assertFalse(
            PlayerVisibilityRules.isVisibleTo(viewer, unrepresentable),
            "delta=+16 aliases to -16 on the wire and must not be visible",
        )
        assertTrue(PlayerVisibilityRules.isVisibleTo(viewer, maxPositive), "delta=+15 is the max representable positive delta")
        assertTrue(PlayerVisibilityRules.isVisibleTo(viewer, maxNegative), "delta=-16 round-trips correctly and must be visible")
    }

    @Test
    fun `the same bound applies on the y axis`() {
        val viewer = client(1, "viewer", 3200, 3200)
        val unrepresentable = client(2, "unrepresentable", 3200, 3216)
        val maxPositive = client(3, "max-positive", 3200, 3215)

        assertFalse(PlayerVisibilityRules.isVisibleTo(viewer, unrepresentable))
        assertTrue(PlayerVisibilityRules.isVisibleTo(viewer, maxPositive))
    }

    private fun client(slot: Int, name: String, x: Int, y: Int): Client =
        Client(EmbeddedChannel(), slot).apply {
            playerName = name
            moveTo(x, y, 0)
            loaded = true
            initialized = true
            isActive = true
            setSynchronizationReady(true)
            clients += this
        }
}
