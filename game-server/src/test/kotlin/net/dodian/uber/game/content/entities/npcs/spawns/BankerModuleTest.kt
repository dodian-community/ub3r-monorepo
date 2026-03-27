package net.dodian.uber.game.content.entities.npcs.spawns

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BankerModuleTest {
    @Test
    fun `banker module uses generated spawn coverage`() {
        assertEquals(BankerGenerated.entries.size, Banker.entries.size)
        assertTrue(Banker.entries.size > 10, "Banker entries should include full generated coverage.")
    }

    @Test
    fun `banker module includes yanille banker coordinates`() {
        val expected = setOf(2615 to 3094, 2615 to 3092, 2615 to 3091)
        val found = Banker.entries.filter { it.npcId in setOf(394, 395, 7677) && (it.x to it.y) in expected }
        assertEquals(expected.size, found.size)
    }
}
