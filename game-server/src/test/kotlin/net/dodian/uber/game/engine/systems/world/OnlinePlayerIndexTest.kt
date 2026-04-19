package net.dodian.uber.game.engine.systems.world

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OnlinePlayerIndexTest {
    @AfterEach
    fun tearDown() {
        PlayerRegistry.playersOnline.clear()
    }

    @Test
    fun `refresh indexes only active online players`() {
        val active = registerClient(slot = 1, key = 1001L, active = true, disconnected = false)
        registerClient(slot = 2, key = 1002L, active = false, disconnected = false)
        registerClient(slot = 3, key = 1003L, active = true, disconnected = true)

        val index = OnlinePlayerIndex()
        index.refresh()

        assertEquals(1, index.playerCount())
        assertEquals(active, index.byDbId(active.dbId))
    }

    private fun registerClient(slot: Int, key: Long, active: Boolean, disconnected: Boolean): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.isActive = active
        client.initialized = true
        client.disconnected = disconnected
        client.dbId = key.toInt()
        PlayerRegistry.playersOnline[key] = client
        return client
    }
}
