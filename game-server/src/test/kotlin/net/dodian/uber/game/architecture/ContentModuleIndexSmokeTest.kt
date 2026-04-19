package net.dodian.uber.game.architecture

import net.dodian.uber.game.api.plugin.ContentModuleIndex
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentModuleIndexSmokeTest {
    @Test
    fun `content module index bootstraps and contains canonical modules`() {
        assertFalse(ContentModuleIndex.pluginCatalog.isEmpty(), "plugin catalog should not be empty")
        assertFalse(ContentModuleIndex.contentBootstraps.isEmpty(), "content bootstraps should not be empty")
        assertFalse(ContentModuleIndex.skillPlugins.isEmpty(), "skill plugins should not be empty")
        assertTrue(
            ContentModuleIndex.commandContents.any { it::class.java.name.contains(".game.command.") },
            "expected canonical command content entries",
        )
        assertTrue(
            ContentModuleIndex.objectContents.any { it.first.isNotBlank() },
            "expected canonical object content entries",
        )
    }
}
