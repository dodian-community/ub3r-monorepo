package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Woodcutting, driving the same
 * packet-equivalent [RealWorldHarness.objectOption] entry point a real client uses and
 * advancing real game ticks. Complements (does not replace) the module-level
 * WoodcuttingModuleRuntimeTest, which exercises the same module against FakeSkillPlayer.
 *
 * Ids sourced from plugins/skills/woodcutting/src/main/resources/woodcutting/{trees,axes}.toml.
 */
class WoodcuttingRealWorldTest : RealWorldSkillTest() {
    private val normalTreeId = 1276
    private val logItemId = 1511
    private val bronzeAxeId = 1351

    private val oakTreeId = 10820
    private val oakLogItemId = 1521
    private val oakRequiredLevel = 15

    @Test
    fun `chopping a tree with an axe yields logs and xp`() {
        val player = spawnPlayer("WoodcutterOne", 3220, 3218)
        RealWorldHarness.setSkillLevel(player, Skill.WOODCUTTING, 1)
        RealWorldHarness.giveItem(player, bronzeAxeId, 1)
        spawnObject(normalTreeId, 3220, 3219)

        RealWorldHarness.objectOption(player, normalTreeId, option = 1, x = 3220, y = 3219)
        val gotLog = RealWorldHarness.tickUntil(maxTicks = 50) { player.getInvAmt(logItemId) > 0 }

        assertTrue(gotLog, "expected at least one log within 50 ticks")
        assertTrue(player.getExperience(Skill.WOODCUTTING) > 0, "expected woodcutting xp to be gained")
    }

    @Test
    fun `missing an axe refuses the action`() {
        val player = spawnPlayer("WoodcutterNoAxe", 3220, 3221)
        RealWorldHarness.setSkillLevel(player, Skill.WOODCUTTING, 1)
        spawnObject(normalTreeId, 3220, 3222)

        RealWorldHarness.objectOption(player, normalTreeId, option = 1, x = 3220, y = 3222)
        RealWorldHarness.tick(10)

        assertEquals(0, player.getInvAmt(logItemId))
        assertEquals(0, player.getExperience(Skill.WOODCUTTING))
    }

    @Test
    fun `insufficient level refuses the action`() {
        val player = spawnPlayer("WoodcutterLowLevel", 3220, 3224)
        RealWorldHarness.setSkillLevel(player, Skill.WOODCUTTING, 1)
        RealWorldHarness.giveItem(player, bronzeAxeId, 1)
        spawnObject(oakTreeId, 3220, 3225)

        RealWorldHarness.objectOption(player, oakTreeId, option = 1, x = 3220, y = 3225)
        RealWorldHarness.tick(10)

        assertEquals(0, player.getInvAmt(oakLogItemId))
        assertEquals(0, player.getExperience(Skill.WOODCUTTING))
        assertTrue(oakRequiredLevel > 1, "sanity check: oak must require a higher level than the test player has")
    }

    /**
     * This ruleset's trees never deplete (no stump/respawn state exists in
     * GatheringPipeline/WoodcuttingModule), unlike vanilla OSRS - so there's no
     * "object depleted" world state to assert here. Walking away mid-action is this
     * codebase's real, observable substitute: GatheringSpotBuilder.cycleOnce checks
     * SkillWorld.withinObjectBoundary every cycle and stops the loop the moment the
     * player leaves the tree's boundary, before any further log can be gained.
     */
    @Test
    fun `walking away cancels the gathering loop`() {
        val player = spawnPlayer("WoodcutterWalksAway", 3220, 3227)
        RealWorldHarness.setSkillLevel(player, Skill.WOODCUTTING, 1)
        RealWorldHarness.giveItem(player, bronzeAxeId, 1)
        spawnObject(normalTreeId, 3220, 3228)

        RealWorldHarness.objectOption(player, normalTreeId, option = 1, x = 3220, y = 3228)
        val gotFirstLog = RealWorldHarness.tickUntil(maxTicks = 50) { player.getInvAmt(logItemId) > 0 }
        assertTrue(gotFirstLog, "expected the action to be actively yielding before we test cancellation")
        val logsBeforeWalkingAway = player.getInvAmt(logItemId)

        player.moveTo(3400, 3400, 0)
        RealWorldHarness.tick(15)

        assertEquals(
            logsBeforeWalkingAway,
            player.getInvAmt(logItemId),
            "expected no further logs once the player left the tree's boundary",
        )
    }
}
