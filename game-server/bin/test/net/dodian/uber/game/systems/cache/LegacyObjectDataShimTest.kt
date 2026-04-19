package net.dodian.uber.game.systems.cache

import net.dodian.cache.`object`.GameObjectData
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LegacyObjectDataShimTest {
    @Test
    fun `forId always returns fallback definition`() {
        val data = GameObjectData.forId(999999)
        assertNotNull(data)
        assertTrue(data.name.isNotBlank())
    }
}
