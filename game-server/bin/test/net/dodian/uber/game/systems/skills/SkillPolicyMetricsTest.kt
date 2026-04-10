package net.dodian.uber.game.systems.skills

import net.dodian.uber.game.systems.action.PolicyPreset
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkillPolicyMetricsTest {
    @Test
    fun `policy metrics aggregate by preset route and result`() {
        SkillPolicyMetrics.record(PolicyPreset.GATHERING, SkillPolicyRoute.OBJECT, SkillPolicyResult.HANDLED)
        SkillPolicyMetrics.record(PolicyPreset.GATHERING, SkillPolicyRoute.OBJECT, SkillPolicyResult.HANDLED)
        SkillPolicyMetrics.record(PolicyPreset.PRODUCTION, SkillPolicyRoute.ACTION_CYCLE, SkillPolicyResult.CANCELLED)

        val snapshot = SkillPolicyMetrics.snapshot()
        assertTrue(snapshot.any { it.key.preset == PolicyPreset.GATHERING && it.key.route == SkillPolicyRoute.OBJECT && it.key.result == SkillPolicyResult.HANDLED && it.value >= 2L })
        assertTrue(snapshot.any { it.key.preset == PolicyPreset.PRODUCTION && it.key.route == SkillPolicyRoute.ACTION_CYCLE && it.key.result == SkillPolicyResult.CANCELLED && it.value >= 1L })
    }
}

