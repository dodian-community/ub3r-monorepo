package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Crafting's gem-cutting flow -
 * item-on-item like Firemaking, executing directly (no "make X" menu, unlike Fletching/
 * Herblore's item-on-item recipes). Complements (does not replace) the module-level
 * CraftingModuleRuntimeTest. Object-click (spinning wheel) coverage is not included here.
 *
 * Ids sourced from CraftingModule.CHISEL_ITEM_ID and
 * plugins/skills/crafting/src/main/resources/crafting/recipes.toml.
 */
class CraftingRealWorldTest : RealWorldSkillTest() {
    private val chiselId = 1755
    private val uncutOpalId = 1625
    private val cutOpalId = 1609

    private val uncutSapphireId = 1623
    private val sapphireRequiredLevel = 20

    @Test
    fun `cutting a gem with a chisel consumes the uncut gem and yields xp`() {
        val player = spawnPlayer("CrafterOne", 3300, 3218)
        RealWorldHarness.setSkillLevel(player, Skill.CRAFTING, 1)
        RealWorldHarness.giveItem(player, chiselId, 1)
        RealWorldHarness.giveItem(player, uncutOpalId, 1)

        RealWorldHarness.itemOnItem(player, chiselId, uncutOpalId)
        RealWorldHarness.tick(3)

        assertEquals(0, player.getInvAmt(uncutOpalId), "expected the uncut gem to be consumed")
        assertTrue(player.getInvAmt(cutOpalId) > 0, "expected a cut opal")
        assertTrue(player.getExperience(Skill.CRAFTING) > 0, "expected crafting xp to be gained")
    }

    @Test
    fun `insufficient level refuses the cut`() {
        val player = spawnPlayer("CrafterLowLevel", 3300, 3221)
        RealWorldHarness.setSkillLevel(player, Skill.CRAFTING, 1)
        RealWorldHarness.giveItem(player, chiselId, 1)
        RealWorldHarness.giveItem(player, uncutSapphireId, 1)

        RealWorldHarness.itemOnItem(player, chiselId, uncutSapphireId)
        RealWorldHarness.tick(3)

        assertEquals(1, player.getInvAmt(uncutSapphireId), "expected the uncut gem not to be consumed on a refused cut")
        assertEquals(0, player.getExperience(Skill.CRAFTING))
        assertTrue(sapphireRequiredLevel > 1, "sanity check: this gem must require a higher level than the test player has")
    }
}
