package net.dodian.uber.game.systems.api.content

import kotlin.reflect.full.findAnnotation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import net.dodian.uber.game.tasks.TickTasks

class ContentSchedulingDeprecationTest {
    @Test
    fun `content coroutine facade is deprecated in favor of content scheduling`() {
        val deprecated = ContentCoroutines::class.findAnnotation<Deprecated>()
        assertNotNull(deprecated)
        assertEquals("ContentScheduling", deprecated?.replaceWith?.expression)
    }

    @Test
    fun `tick tasks facade is deprecated in favor of content scheduling`() {
        val deprecated = TickTasks::class.findAnnotation<Deprecated>()
        assertNotNull(deprecated)
        assertEquals("ContentScheduling", deprecated?.replaceWith?.expression)
    }
}
