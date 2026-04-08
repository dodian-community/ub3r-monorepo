package net.dodian.uber.game.systems.api.content

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.reflect.full.findAnnotation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import net.dodian.uber.game.tasks.TickTasks

class ContentSchedulingDeprecationTest {
    @Test
    fun `legacy content coroutine facade file is removed`() {
        assertFalse(
            Files.exists(Paths.get("src/main/kotlin/net/dodian/uber/game/systems/api/content/ContentCoroutines.kt")),
        )
    }

    @Test
    fun `tick tasks facade is deprecated in favor of content scheduling`() {
        val deprecated = TickTasks::class.findAnnotation<Deprecated>()
        assertNotNull(deprecated)
        assertEquals("ContentScheduling", deprecated?.replaceWith?.expression)
    }
}
