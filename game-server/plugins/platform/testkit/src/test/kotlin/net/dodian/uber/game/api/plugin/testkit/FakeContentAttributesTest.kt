package net.dodian.uber.game.api.plugin.testkit

import net.dodian.uber.game.api.content.ContentAttributeKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FakeContentAttributesTest {
    @Test
    fun `attributes remain owner scoped and removable`() {
        val attributes = FakeContentAttributes()
        val first = ContentAttributeKey<Int>("first", "state")
        val second = ContentAttributeKey<Int>("second", "state")

        attributes.put(first, 1)
        attributes.put(second, 2)

        assertEquals(1, attributes.get(first))
        assertEquals(2, attributes.get(second))
        attributes.remove(first)
        assertNull(attributes.get(first))
        assertEquals(2, attributes.get(second))
    }
}
