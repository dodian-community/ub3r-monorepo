package net.dodian.uber.game.skill.thieving

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.api.plugin.PluginRegistry
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThievingCageBindingTest {
    @Test
    fun `alternate cage id is registered and handled`() {
        assertTrue(20885 in ThievingObjectComponents.chestObjects)
        assertNotNull(ThievingDefinition.forId(20885))

        PluginRegistry.resetForTests()
        PluginRegistry.bootstrap()

        val client = Client(EmbeddedChannel(), 1)
        val binding = requireNotNull(PluginRegistry.currentSkills().objectBinding(option = 1, objectId = 20885))
        val handled = binding.handler(client, 20885, Position(2604, 3100, 0), null)
        assertTrue(handled)

        PluginRegistry.resetForTests()
    }
}
