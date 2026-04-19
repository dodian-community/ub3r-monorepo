package net.dodian.uber.game.persistence.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WorldDbPollServiceQueryHelperTest {
    @Test
    fun `normalized ids deduplicate and discard non-positive values`() {
        val normalized = WorldDbPollService.normalizedIds(listOf(7, -1, 0, 7, 9, 9, 12))
        assertEquals(listOf(7, 9, 12), normalized)
    }

    @Test
    fun `in clause placeholders track id count`() {
        assertEquals("?,?,?", WorldDbPollService.inClausePlaceholders(listOf(1, 2, 3)))
        assertEquals("?", WorldDbPollService.inClausePlaceholders(emptyList()))
    }
}
