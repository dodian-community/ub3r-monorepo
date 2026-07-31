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

    @Test
    fun `combining two 1-dose potions merges them into a 2-dose plus an empty vial`() {
        val player = spawnPlayer("HerbalistDose", 3290, 3224)
        RealWorldHarness.giveItem(player, 119, 2)

        RealWorldHarness.itemOnItem(player, 119, 119)

        assertEquals(0, player.getInvAmt(119))
        assertEquals(1, player.getInvAmt(117))
        assertEquals(1, player.getInvAmt(229))
    }

    @Test
    fun `mixing a super combat potion consumes all 4 ingredients and grants xp`() {
        val player = spawnPlayer("HerbalistSuperCombat", 3290, 3227)
        RealWorldHarness.setSkillLevel(player, Skill.HERBLORE, 88)
        RealWorldHarness.giveItem(player, 269, 1)
        RealWorldHarness.giveItem(player, 2436, 1)
        RealWorldHarness.giveItem(player, 2440, 1)
        RealWorldHarness.giveItem(player, 2442, 1)

        RealWorldHarness.itemOnItem(player, 269, 2436)

        assertEquals(1, player.getInvAmt(12695), "expected a super combat potion")
        assertEquals(0, player.getInvAmt(269))
        assertEquals(0, player.getInvAmt(2436))
        assertEquals(0, player.getInvAmt(2440))
        assertEquals(0, player.getInvAmt(2442))
        assertTrue(player.getExperience(Skill.HERBLORE) > 0)
    }

    // --- Zahur: herb-cleaner batch grind + bulk potion decanting --------------------------------
    // Both flows operate on *noted* items, resolved from the real cache via
    // Client.getNotedItem - unlike the FakeSkillPlayer tests (identity passthrough), this
    // exercises the genuine noted/unnoted item pairing.

    private val zahurNpcId = 4753
    private val grimyGuamId = 199
    private val cleanGuamProduct = 249

    @Test
    fun `zahur cleans a noted grimy herb for a fee via the herb cleaner menu`() {
        val player = spawnPlayer("HerbalistZahur", 3184, 3435)
        val zahur = spawnNpc(zahurNpcId, 3184, 3436)
        val notedGrimyGuam = player.getNotedItem(grimyGuamId)
        RealWorldHarness.giveItem(player, notedGrimyGuam, 5)
        RealWorldHarness.giveItem(player, 995, 1000)

        RealWorldHarness.npcOption(player, zahur, 4) // "clean herbs" direct option
        RealWorldHarness.tick(5)
        RealWorldHarness.button(player, 2461) // pick the (only) herb shown in the menu
        RealWorldHarness.enterAmount(player, 3)

        val notedCleanGuam = player.getNotedItem(cleanGuamProduct)
        assertEquals(2, player.getInvAmt(notedGrimyGuam), "expected 3 of the 5 noted grimy herbs to be consumed")
        assertEquals(3, player.getInvAmt(notedCleanGuam), "expected 3 noted clean herbs produced")
        assertEquals(1000 - 3 * 200, player.getInvAmt(995))
    }

    @Test
    fun `zahur decants noted potions in bulk via the decant menu`() {
        val player = spawnPlayer("HerbalistDecant", 3184, 3438)
        val zahur = spawnNpc(zahurNpcId, 3184, 3439)
        val notedOneDose = player.getNotedItem(119)
        RealWorldHarness.giveItem(player, notedOneDose, 2)

        RealWorldHarness.npcOption(player, zahur, 3) // "decant" direct option
        RealWorldHarness.tick(5)
        RealWorldHarness.dialogueOption(player, 3) // "Two dose"

        val notedTwoDose = player.getNotedItem(117)
        // 230 is already the noted empty-vial item id (HerbloreModule.NOTED_EMPTY_VIAL_ID is
        // hardcoded to it, matching legacy) - getNotedItem(230) would incorrectly try to note
        // an already-noted item and return 0.
        assertEquals(0, player.getInvAmt(notedOneDose))
        assertEquals(1, player.getInvAmt(notedTwoDose))
        assertEquals(1, player.getInvAmt(230))
    }
}
