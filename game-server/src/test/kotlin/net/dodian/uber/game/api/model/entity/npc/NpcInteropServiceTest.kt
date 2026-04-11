package net.dodian.uber.game.model.entity.npc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NpcInteropServiceTest {
    @Test
    fun `npc definition maps to legacy npc data shape`() {
        val parsed =
            NpcDefinitionData(
                id = 1234,
                name = "Test_Npc",
                examine = "Exam_text",
                attackEmote = 111,
                deathEmote = 222,
                respawn = 75,
                combat = 52,
                size = 2,
                defence = 10,
                attack = 20,
                strength = 30,
                hitpoints = 40,
                ranged = 50,
                magic = 60,
            )

        val legacy = NpcDataInterop.toLegacy(parsed)
        assertEquals("Test Npc", legacy.name)
        assertEquals("Exam text", legacy.examine)
        assertEquals(111, legacy.attackEmote)
        assertEquals(222, legacy.deathEmote)
        assertEquals(75, legacy.respawn)
        assertEquals(52, legacy.combat)
        assertEquals(2, legacy.size)
        assertEquals(10, legacy.level[0])
        assertEquals(20, legacy.level[1])
        assertEquals(30, legacy.level[2])
        assertEquals(40, legacy.level[3])
        assertEquals(50, legacy.level[4])
        assertEquals(0, legacy.level[5])
        assertEquals(60, legacy.level[6])
    }

    @Test
    fun `drop math roll amount stays in bounds`() {
        repeat(100) {
            val rolled = NpcDropMath.rollAmount(2, 5)
            assertTrue(rolled in 2..5)
        }
        assertEquals(7, NpcDropMath.rollAmount(7, 7))
    }
}
