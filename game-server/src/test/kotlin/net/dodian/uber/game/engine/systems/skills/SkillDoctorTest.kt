package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.content.skills.runtime.parity.SkillDoctor
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkillDoctorTest {
    @Test
    fun `skill doctor validates migrated skill platform`() {
        val report = SkillDoctor.snapshot()
        assertTrue(report.isClean, "Expected SkillDoctor report to be clean, found: ${report.findings}")
    }
}

