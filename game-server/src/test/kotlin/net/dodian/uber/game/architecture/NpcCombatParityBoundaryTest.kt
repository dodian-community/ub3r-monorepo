package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NpcCombatParityBoundaryTest {
    @Test
    fun `npc combat flow uses npc-owned eligibility and preserves combat-facing`() {
        val source =
            Files.readString(
                Path.of("src/main/java/net/dodian/uber/game/model/entity/npc/Npc.java"),
            )

        assertTrue(source.contains("if (!hasActiveCombatTargets() && defaultFace >= 0"))
        assertTrue(source.contains("private boolean isEligibleNpcAttackTarget(Client player, boolean requireInRange)"))
        assertTrue(source.contains("if (!isEligibleNpcAttackTarget(candidate, fighting))"))
        assertFalse(source.contains("isActivelyTargetingThisNpc("))
    }
}
