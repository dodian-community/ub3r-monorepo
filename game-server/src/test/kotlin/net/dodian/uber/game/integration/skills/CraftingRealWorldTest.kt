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

    @Test
    fun `talking to the tanner and clicking an amount button tans a hide`() {
        val player = spawnPlayer("CrafterTanner", 3300, 3224)
        val tanner = spawnNpc(5809, 3301, 3224)
        RealWorldHarness.giveItem(player, 1753, 1)
        RealWorldHarness.giveItem(player, 995, 1000)

        RealWorldHarness.npcOption(player, tanner, 1)
        RealWorldHarness.tick(5)
        val opened = RealWorldHarness.button(player, 57227)

        assertTrue(opened, "expected the tanning amount-button to be handled")
        assertEquals(1, player.getInvAmt(1745), "expected a tanned green dragonhide leather")
        assertEquals(0, player.getInvAmt(1753))
        assertEquals(0, player.getInvAmt(995))
    }

    @Test
    fun `molten glass and bucket open the picker and a button click blows a vial`() {
        val player = spawnPlayer("CrafterGlass", 3300, 3227)
        RealWorldHarness.giveItem(player, 1775, 1)
        RealWorldHarness.giveItem(player, 1785, 1)

        RealWorldHarness.itemOnItem(player, 1775, 1785)
        val started = RealWorldHarness.button(player, 44207)
        RealWorldHarness.tick(3)

        assertTrue(started, "expected the glass amount-button to be handled")
        assertEquals(0, player.getInvAmt(1775))
        assertEquals(1, player.getInvAmt(229), "expected a blown vial")
        assertTrue(player.getExperience(Skill.CRAFTING) > 0)
    }

    @Test
    fun `crystal key halves combine into a crystal key`() {
        val player = spawnPlayer("CrafterKey", 3300, 3230)
        RealWorldHarness.setSkillLevel(player, Skill.CRAFTING, 60)
        RealWorldHarness.giveItem(player, 2382, 1)
        RealWorldHarness.giveItem(player, 2383, 1)

        RealWorldHarness.itemOnItem(player, 2382, 2383)

        assertEquals(1, player.getInvAmt(989))
        assertEquals(0, player.getInvAmt(2382))
        assertEquals(0, player.getInvAmt(2383))
    }
}
