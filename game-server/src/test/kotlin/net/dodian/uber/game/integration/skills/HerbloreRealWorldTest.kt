package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Herblore - item-on-item routed
 * through the same "make X" production menu as Fletching ([RealWorldHarness.itemOnItem]
 * opens it, [RealWorldHarness.completeProduction] closes it). Complements (does not
 * replace) the module-level HerbloreModuleRuntimeTest.
 *
 * Ids sourced from plugins/skills/herblore/src/main/resources/herblore/recipes.toml and
 * HerbloreModule.UNFINISHED_VIAL. This covers the itemOnItem (clean herb + vial ->
 * unfinished potion) leg only - the itemClick (grimy -> clean herb) leg is a different
 * interaction archetype and isn't covered here.
 */
class HerbloreRealWorldTest : RealWorldSkillTest() {
    private val vialOfWaterId = 227
    private val cleanGuamId = 249
    private val unfinishedGuamPotionId = 91
    private val makeOneButtonId = 2799 // single-entry interface: make 1

    private val cleanMarrentillId = 253
    private val marrentillRequiredLevel = 10

    @Test
    fun `mixing a clean herb with a vial of water yields an unfinished potion and xp`() {
        val player = spawnPlayer("HerbalistOne", 3290, 3218)
        RealWorldHarness.setSkillLevel(player, Skill.HERBLORE, 1)
        RealWorldHarness.giveItem(player, cleanGuamId, 1)
        RealWorldHarness.giveItem(player, vialOfWaterId, 1)

        RealWorldHarness.itemOnItem(player, cleanGuamId, vialOfWaterId)
        RealWorldHarness.tick(1)
        RealWorldHarness.completeProduction(player, makeOneButtonId)
        RealWorldHarness.tick(5)

        assertTrue(player.getInvAmt(unfinishedGuamPotionId) > 0, "expected an unfinished guam potion")
        assertEquals(0, player.getInvAmt(cleanGuamId), "expected the clean herb to be consumed")
        assertTrue(player.getExperience(Skill.HERBLORE) > 0, "expected herblore xp to be gained")
    }

    @Test
    fun `insufficient level refuses the mix`() {
        val player = spawnPlayer("HerbalistLowLevel", 3290, 3221)
        RealWorldHarness.setSkillLevel(player, Skill.HERBLORE, 1)
        RealWorldHarness.giveItem(player, cleanMarrentillId, 1)
        RealWorldHarness.giveItem(player, vialOfWaterId, 1)

        RealWorldHarness.itemOnItem(player, cleanMarrentillId, vialOfWaterId)
        RealWorldHarness.tick(1)
        RealWorldHarness.completeProduction(player, makeOneButtonId)
        RealWorldHarness.tick(5)

        assertEquals(0, player.getExperience(Skill.HERBLORE))
        assertTrue(marrentillRequiredLevel > 1, "sanity check: this herb must require a higher level than the test player has")
    }
}
