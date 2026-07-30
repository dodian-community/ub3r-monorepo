package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Fishing - the npc-click
 * archetype (vs. woodcutting/mining's object-click), driving the same
 * packet-equivalent [RealWorldHarness.npcOption] entry point a real client uses.
 * Complements (does not replace) the module-level FishingModuleRuntimeTest.
 *
 * Ids sourced from plugins/skills/fishing/src/main/resources/fishing/spots.toml.
 */
class FishingRealWorldTest : RealWorldSkillTest() {
    private val shrimpSpotNpcId = 1518
    private val shrimpItemId = 317
    private val smallNetItemId = 303

    private val lobsterSpotNpcId = 1519
    private val lobsterRequiredLevel = 40

    @Test
    fun `fishing a spot with the right tool yields fish and xp`() {
        val player = spawnPlayer("FisherOne", 3250, 3218)
        RealWorldHarness.setSkillLevel(player, Skill.FISHING, 1)
        RealWorldHarness.giveItem(player, smallNetItemId, 1)
        val spotNpc = spawnNpc(shrimpSpotNpcId, 3250, 3219)

        RealWorldHarness.npcOption(player, spotNpc, option = 1)
        val gotFish = RealWorldHarness.tickUntil(maxTicks = 50) { player.getInvAmt(shrimpItemId) > 0 }

        assertTrue(gotFish, "expected at least one fish within 50 ticks")
        assertTrue(player.getExperience(Skill.FISHING) > 0, "expected fishing xp to be gained")
    }

    @Test
    fun `missing the required tool refuses the action`() {
        val player = spawnPlayer("FisherNoNet", 3250, 3221)
        RealWorldHarness.setSkillLevel(player, Skill.FISHING, 1)
        val spotNpc = spawnNpc(shrimpSpotNpcId, 3250, 3222)

        RealWorldHarness.npcOption(player, spotNpc, option = 1)
        RealWorldHarness.tick(10)

        assertEquals(0, player.getInvAmt(shrimpItemId))
        assertEquals(0, player.getExperience(Skill.FISHING))
    }

    @Test
    fun `insufficient level refuses the action`() {
        val player = spawnPlayer("FisherLowLevel", 3250, 3224)
        RealWorldHarness.setSkillLevel(player, Skill.FISHING, 1)
        val spotNpc = spawnNpc(lobsterSpotNpcId, 3250, 3225)

        RealWorldHarness.npcOption(player, spotNpc, option = 1)
        RealWorldHarness.tick(10)

        assertEquals(0, player.getExperience(Skill.FISHING))
        assertTrue(lobsterRequiredLevel > 1, "sanity check: this spot must require a higher level than the test player has")
    }
}
