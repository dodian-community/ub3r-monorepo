package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Firemaking - the item-on-item
 * archetype (tinderbox used on logs), driving the same packet-equivalent
 * [RealWorldHarness.itemOnItem] entry point (via ItemCombinationService) a real client
 * uses. Complements (does not replace) the module-level FiremakingModuleRuntimeTest.
 *
 * Ids sourced from FiremakingModule.TINDERBOX and
 * plugins/skills/firemaking/src/main/resources/firemaking/logs.toml.
 */
class FiremakingRealWorldTest : RealWorldSkillTest() {
    private val tinderboxId = 590
    private val normalLogsId = 1511

    private val oakLogsId = 1521
    private val oakRequiredLevel = 15

    @Test
    fun `lighting logs with a tinderbox consumes the log and yields xp`() {
        val player = spawnPlayer("FiremakerOne", 3270, 3218)
        RealWorldHarness.setSkillLevel(player, Skill.FIREMAKING, 1)
        RealWorldHarness.giveItem(player, tinderboxId, 1)
        RealWorldHarness.giveItem(player, normalLogsId, 1)

        RealWorldHarness.itemOnItem(player, tinderboxId, normalLogsId)
        RealWorldHarness.tick(3)

        assertEquals(0, player.getInvAmt(normalLogsId), "expected the log to be consumed")
        assertTrue(player.getExperience(Skill.FIREMAKING) > 0, "expected firemaking xp to be gained")
    }

    @Test
    fun `insufficient level refuses the action`() {
        val player = spawnPlayer("FiremakerLowLevel", 3270, 3221)
        RealWorldHarness.setSkillLevel(player, Skill.FIREMAKING, 1)
        RealWorldHarness.giveItem(player, tinderboxId, 1)
        RealWorldHarness.giveItem(player, oakLogsId, 1)

        RealWorldHarness.itemOnItem(player, tinderboxId, oakLogsId)
        RealWorldHarness.tick(3)

        assertEquals(1, player.getInvAmt(oakLogsId), "expected the log not to be consumed on a refused burn")
        assertEquals(0, player.getExperience(Skill.FIREMAKING))
        assertTrue(oakRequiredLevel > 1, "sanity check: this log must require a higher level than the test player has")
    }
}
