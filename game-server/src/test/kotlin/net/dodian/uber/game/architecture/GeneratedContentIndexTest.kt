package net.dodian.uber.game.architecture

import net.dodian.uber.game.systems.content.ContentModuleIndex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GeneratedContentIndexTest {
    @Test
    fun `generated command contents include expected module groups`() {
        val moduleNames = ContentModuleIndex.commandContents.map { it::class.java.name }.toSet()

        assertTrue(moduleNames.any { it.endsWith("content.commands.dev.DevDebugCommands") }, "Expected dev command module in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.commands.beta.BetaOnlyCommands") }, "Expected beta command module in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.commands.player.PlayerCommands") }, "Expected player command module in generated index")
        assertTrue(moduleNames.any { it.endsWith("content.commands.admin.StaffCommands") }, "Expected admin command module in generated index")
    }

    @Test
    fun `generated content bootstraps include core registries and are unique`() {
        val bootstraps = ContentModuleIndex.contentBootstraps
        val ids = bootstraps.map { it.id }

        assertTrue(ids.contains("commands.registry"), "commands registry bootstrap missing")
        assertTrue(ids.contains("items.registry"), "items registry bootstrap missing")
        assertTrue(ids.contains("npcs.registry"), "npcs registry bootstrap missing")
        assertTrue(ids.contains("objects.registry"), "objects registry bootstrap missing")
        assertEquals(ids.size, ids.toSet().size, "duplicate content bootstrap ids found")

        bootstraps.forEach { it.bootstrap() }
    }
}
