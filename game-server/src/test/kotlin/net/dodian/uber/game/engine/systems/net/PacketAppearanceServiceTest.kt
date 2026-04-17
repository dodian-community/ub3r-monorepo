package net.dodian.uber.game.engine.systems.net

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PacketAppearanceServiceTest {
    @Test
    fun `appearance handler sets looks and appearance update flag`() {
        val client = client(1981)
        client.updateFlags.setRequired(UpdateFlag.APPEARANCE, false)

        val looks = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        PacketAppearanceService.handleAppearanceChange(client, looks)

        assertArrayEquals(looks, client.playerLooks)
        assertTrue(client.updateFlags.isRequired(UpdateFlag.APPEARANCE))
    }

    private fun client(slot: Int): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.playerName = "appearance-$slot"
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.validClient = true
        client.teleportTo(3096, 3490, 0)
        return client
    }
}
