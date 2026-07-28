package net.dodian.uber.game.engine.systems.skills

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SkillBetaTelemetryTest {
    @AfterEach
    fun reset() = SkillBetaTelemetry.resetForTests()

    @Test
    fun `groups live beta outcomes by module route and result`() {
        SkillBetaTelemetry.recordOutcome("skill.mining", SkillPolicyRoute.OBJECT, "HANDLED")
        SkillBetaTelemetry.recordOutcome("skill.mining", SkillPolicyRoute.OBJECT, "HANDLED")
        SkillBetaTelemetry.recordOutcome("skill.mining", SkillPolicyRoute.OBJECT, "ERROR")

        assertEquals(
            mapOf(
                "skill.mining:object:error" to 1L,
                "skill.mining:object:handled" to 2L,
            ),
            SkillBetaTelemetry.snapshot(),
        )
    }
}
