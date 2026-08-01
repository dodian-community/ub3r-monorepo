package net.dodian.uber.game.model.entity.player

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.combat.shoot
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.netty.codec.ByteMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ClientWorldEffectScopeTest {
    private val clients = mutableListOf<Client>()

    @AfterEach
    fun tearDown() {
        clients.forEach {
            it.saveNeeded = false
            it.destruct()
        }
        clients.clear()
        PlayerRegistry.players.fill(null)
        PlayerRegistry.playersOnline.clear()
    }

    @Test
    fun `animation and still graphic never cross planes`() {
        val caster = client(1, 3200, 3200, 0)
        val upstairs = client(2, 3200, 3200, 1)
        PlayerRegistry.players[1] = caster
        PlayerRegistry.players[2] = upstairs

        caster.animation(1, Position(3200, 3200, 0))
        caster.stillgfx(1, Position(3200, 3200, 0), 0, true)
        caster.flushOutbound()
        upstairs.flushOutbound()

        assertNull((upstairs.channel as EmbeddedChannel).readOutbound<ByteMessage>())
    }

    @Test
    fun `replace object does not emit packets outside the viewer scene`() {
        val viewer = client(1, 3500, 3500, 0)
        PlayerRegistry.players[1] = viewer

        viewer.ReplaceObject(3200, 3200, 1234, 0, 0)
        viewer.flushOutbound()

        assertNull((viewer.channel as EmbeddedChannel).readOutbound<ByteMessage>())
    }

    @Test
    fun `projectile never crosses planes`() {
        val caster = client(1, 3200, 3200, 0)
        val upstairs = client(2, 3200, 3200, 1)
        PlayerRegistry.players[1] = caster
        PlayerRegistry.players[2] = upstairs

        caster.shoot("bronze_arrow_travel", Position(3201, 3200, 0))
        caster.flushOutbound()
        upstairs.flushOutbound()

        assertNull((upstairs.channel as EmbeddedChannel).readOutbound<ByteMessage>())
    }

    private fun client(slot: Int, x: Int, y: Int, z: Int): Client =
        Client(EmbeddedChannel(), slot).apply {
            playerName = "client$slot"
            moveTo(x, y, z)
            loaded = true
            initialized = true
            isActive = true
            setSynchronizationReady(true)
            clients += this
        }
}
