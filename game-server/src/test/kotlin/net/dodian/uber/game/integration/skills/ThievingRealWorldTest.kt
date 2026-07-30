package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Thieving's npc pickpocket
 * flow, driving the same packet-equivalent [RealWorldHarness.npcOption] entry point (with
 * click option 3, per ThievingModule.npcClick) a real client uses. This module has no
 * stun/fail-chance mechanic - pickpocketing always succeeds once the level and throttle
 * gates pass, so only the looted item/amount is real, unseeded RNG (rollLoot); xp is
 * deterministic. Complements (does not replace) the module-level ThievingModuleRuntimeTest.
 *
 * Ids sourced from plugins/skills/thieving/src/main/resources/thieving/tables.toml.
 */
class ThievingRealWorldTest : RealWorldSkillTest() {
    private val farmerNpcId = 3114
    private val farmerRequiredLevel = 10
    private val farmerExperience = 800
    private val farmerLootItemId = 314 // feathers, 100% chance, 2-5 amount

    private val masterFarmerNpcId = 5730
    private val masterFarmerRequiredLevel = 70

    @Test
    fun `pickpocketing an npc yields loot and xp`() {
        val player = spawnPlayer("ThiefOne", 3320, 3218)
        RealWorldHarness.setSkillLevel(player, Skill.THIEVING, farmerRequiredLevel)
        val farmer = spawnNpc(farmerNpcId, 3320, 3219)

        RealWorldHarness.npcOption(player, farmer, option = 3)
        val gotLoot = RealWorldHarness.tickUntil(maxTicks = 20) { player.getInvAmt(farmerLootItemId) > 0 }

        assertTrue(gotLoot, "expected feathers within 20 ticks")
        assertEquals(farmerExperience, player.getExperience(Skill.THIEVING), "expected deterministic xp regardless of the loot-amount roll")
    }

    @Test
    fun `insufficient level refuses the pickpocket`() {
        val player = spawnPlayer("ThiefLowLevel", 3320, 3221)
        RealWorldHarness.setSkillLevel(player, Skill.THIEVING, 1)
        val masterFarmer = spawnNpc(masterFarmerNpcId, 3320, 3222)

        RealWorldHarness.npcOption(player, masterFarmer, option = 3)
        RealWorldHarness.tick(10)

        assertEquals(0, player.getExperience(Skill.THIEVING))
        assertTrue(masterFarmerRequiredLevel > 1, "sanity check: this npc must require a higher level than the test player has")
    }
}
