package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.skill.runtime.parity.SkillDoctor
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class SkillDoctorTest {
    @Test
    fun `skill doctor validates migrated skill platform`() {
        val report = SkillDoctor.snapshot()
        assertTrue(report.isClean, "Expected SkillDoctor report to be clean, found: ${report.findings}")
    }

    @Test
    fun `skill doctor enforces prayer slayer thieving route ownership checks`() {
        val source =
            Files.readString(
                Path.of("src/main/kotlin/net/dodian/uber/game/skill/runtime/parity/SkillDoctor.kt"),
            )

        assertTrue(source.contains("Prayer route ownership missing bone click option=1"))
        assertTrue(source.contains("Prayer route ownership missing altar item-on-object"))
        assertTrue(source.contains("Slayer route ownership missing gem click option="))
        assertTrue(source.contains("Slayer route ownership missing mask click option=3"))
        assertTrue(source.contains("Thieving route ownership missing object click option=1"))
        assertTrue(source.contains("Thieving route ownership missing object click option=2"))
    }
}
