package net.dodian.uber.game.engine.processing

import io.netty.channel.embedded.EmbeddedChannel
import java.util.concurrent.atomic.AtomicInteger
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.player.PlayerTickEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EntityProcessorPlayerTickIntegrationTest {
    @AfterEach
    fun tearDown() {
        PlayerRegistry.playersOnline.clear()
        GameEventBus.clear()
    }

    @Test
    fun `player main phase does not post player tick events`() {
        val eventsSeen = AtomicInteger(0)
        GameEventBus.on<PlayerTickEvent>(
            action = {
                eventsSeen.incrementAndGet()
                true
            },
        )

        val player = Client(EmbeddedChannel(), 1)
        player.isActive = true
        player.initialized = true
        PlayerRegistry.playersOnline[1L] = player

        EntityProcessor().runPlayerMainPhase(System.currentTimeMillis())

        assertEquals(0, eventsSeen.get())
    }
}
