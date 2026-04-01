package net.dodian.uber.game.model.player.skills

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SkillsProgressionTest {
    @Test
    fun `level for experience is capped at 99`() {
        assertEquals(99, Skills.getLevelForExperience(Skills.getXPForLevel(99)))
        assertEquals(99, Skills.getLevelForExperience(200_000_000))
    }

    @Test
    fun `xp thresholds map to expected early levels`() {
        assertEquals(1, Skills.getLevelForExperience(0))
        assertEquals(2, Skills.getLevelForExperience(Skills.getXPForLevel(2)))
        assertEquals(99, Skills.getLevelForExperience(Skills.getXPForLevel(99) + 50_000))
    }
}
