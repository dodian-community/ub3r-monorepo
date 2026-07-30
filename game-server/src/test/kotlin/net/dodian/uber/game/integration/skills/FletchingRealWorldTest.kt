package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Fletching - the item-on-item
 * archetype like Firemaking, but routed through a real "make X" production menu
 * ([RealWorldHarness.itemOnItem] opens it, [RealWorldHarness.completeProduction] closes
 * it the same way a real button-click packet does via SkillMultiButtonService). This is
 * the same menu machinery Herblore and Crafting use. Complements (does not replace) the
 * module-level FletchingModuleRuntimeTest.
 *
 * Ids sourced from FletchingModule.knifeIds/shaftRecipe and
 * plugins/skills/fletching/src/main/resources/fletching/bows.toml.
 */
class FletchingRealWorldTest : RealWorldSkillTest() {
    private val knifeId = 946
    private val normalLogsId = 1511
    private val arrowShaftId = 52
    private val makeOneButtonId = 2799 // single-entry interface: make 1

    private val oakLogsId = 1521
    private val unstrungOakShortbowId = 54
    private val oakShortbowRequiredLevel = 20
    private val bowEntryOneMakeOneButtonId = 34174 // two-entry interface: entry 0 (short bow), make 1

    @Test
    fun `cutting logs into arrow shafts consumes a log and yields xp`() {
        val player = spawnPlayer("FletcherOne", 3280, 3218)
        RealWorldHarness.setSkillLevel(player, Skill.FLETCHING, 1)
        RealWorldHarness.giveItem(player, knifeId, 1)
        RealWorldHarness.giveItem(player, normalLogsId, 1)

        RealWorldHarness.itemOnItem(player, knifeId, normalLogsId)
        RealWorldHarness.tick(1)
        val menuOpened = RealWorldHarness.completeProduction(player, makeOneButtonId)
        RealWorldHarness.tick(5)

        assertTrue(menuOpened, "expected the make-x menu to accept the make-1 button")
        assertTrue(player.getInvAmt(arrowShaftId) > 0, "expected at least one arrow shaft")
        assertEquals(0, player.getInvAmt(normalLogsId), "expected the log to be consumed")
        assertTrue(player.getExperience(Skill.FLETCHING) > 0, "expected fletching xp to be gained")
    }

    @Test
    fun `insufficient level refuses the selected bow`() {
        val player = spawnPlayer("FletcherLowLevel", 3280, 3221)
        RealWorldHarness.setSkillLevel(player, Skill.FLETCHING, 1)
        RealWorldHarness.giveItem(player, knifeId, 1)
        RealWorldHarness.giveItem(player, oakLogsId, 1)

        RealWorldHarness.itemOnItem(player, knifeId, oakLogsId)
        RealWorldHarness.tick(1)
        RealWorldHarness.completeProduction(player, bowEntryOneMakeOneButtonId)
        RealWorldHarness.tick(5)

        assertEquals(0, player.getInvAmt(unstrungOakShortbowId), "expected no shortbow to be produced")
        assertEquals(0, player.getExperience(Skill.FLETCHING))
        assertTrue(oakShortbowRequiredLevel > 1, "sanity check: this bow must require a higher level than the test player has")
    }
}
