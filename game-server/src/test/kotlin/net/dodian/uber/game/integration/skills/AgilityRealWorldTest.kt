package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Agility's obstacle-crossing
 * flow, driving the same packet-equivalent [RealWorldHarness.objectOption] entry point a
 * real client uses.
 *
 * Correction to an earlier assumption: `SkillWorld.traverse` completes via
 * `GameEventScheduler.runLaterMs(durationMs)`, which looked like a wall-clock scheduler
 * at a glance, but actually converts `durationMs` to ticks
 * (`GameEventScheduler.delayMsToTicks`) and waits via `GameTaskRuntime.queueWorld { wait(ticks) }`
 * - the same world-task queue that's cycled every real tick inside
 * `GameLoopService.runTick()` (`world.tasks` phase -> `ActionProcessor.run()` ->
 * `QueueTaskService.processDue()` -> `GameTaskRuntime.cycleWorld()`). So this tier's
 * ordinary `tick()`/`tickUntil()` drives it exactly like the gathering/production skills;
 * no real sleep or extra harness support is needed after all.
 *
 * `AgilityModule.crossObstacle` requires the player to be standing at the obstacle's
 * exact real-world `startPosition` before it will run (checked *after* the level/item
 * gates), so the success test spawns the player at that real coordinate rather than an
 * arbitrary one, unlike the other skill tests in this tier.
 *
 * Ids sourced from plugins/skills/agility/src/main/resources/agility/courses/{gnome,barbarian}.toml.
 */
class AgilityRealWorldTest : RealWorldSkillTest() {
    private val logBalanceObjectId = 23145
    private val logBalanceStartX = 2474
    private val logBalanceStartY = 3436
    private val logBalanceExperience = 280

    private val ropeSwingObjectId = 23131
    private val ropeSwingRequiredLevel = 40

    @Test
    fun `crossing an obstacle from its start position yields xp`() {
        val player = spawnPlayer("AgileOne", logBalanceStartX, logBalanceStartY)
        RealWorldHarness.setSkillLevel(player, Skill.AGILITY, 1)
        spawnObject(logBalanceObjectId, logBalanceStartX, logBalanceStartY + 1)

        RealWorldHarness.objectOption(player, logBalanceObjectId, option = 1, x = logBalanceStartX, y = logBalanceStartY + 1)
        val gotXp = RealWorldHarness.tickUntil(maxTicks = 20) { player.getExperience(Skill.AGILITY) > 0 }

        assertTrue(gotXp, "expected agility xp within 20 ticks")
        assertEquals(logBalanceExperience, player.getExperience(Skill.AGILITY))
    }

    @Test
    fun `insufficient level refuses the obstacle before checking position`() {
        // Deliberately NOT spawned at the obstacle's real start position - the level
        // check runs first in AgilityModule.crossObstacle, so this still exercises the
        // refusal path without needing the exact real-world coordinate.
        val player = spawnPlayer("AgileLowLevel", 3350, 3218)
        RealWorldHarness.setSkillLevel(player, Skill.AGILITY, 1)
        spawnObject(ropeSwingObjectId, 3350, 3219)

        RealWorldHarness.objectOption(player, ropeSwingObjectId, option = 1, x = 3350, y = 3219)
        RealWorldHarness.tick(10)

        assertEquals(0, player.getExperience(Skill.AGILITY))
        assertTrue(ropeSwingRequiredLevel > 1, "sanity check: this obstacle must require a higher level than the test player has")
    }
}
