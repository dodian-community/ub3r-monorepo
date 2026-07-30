package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Cooking - the item-on-object
 * archetype (raw food used on a range), driving the same packet-equivalent
 * [RealWorldHarness.itemOnObject] entry point a real client uses. Complements (does not
 * replace) the module-level CookingModuleRuntimeTest.
 *
 * Ids sourced from CookingModule.rangeObjectIds and
 * plugins/skills/cooking/src/main/resources/cooking/recipes.toml. Burning is a real,
 * unseeded RNG roll (CookingModule.cookOnce coerces the burn chance into [0,100]) - the
 * success test sets the player's cooking level far above the raw shrimp requirement so
 * the roll's effective burn chance is clamped to 0, making the outcome deterministic
 * without needing an RNG seam.
 */
class CookingRealWorldTest : RealWorldSkillTest() {
    private val rangeObjectId = 26181
    private val rawShrimpId = 317
    private val cookedShrimpId = 315

    private val rawLobsterId = 377
    private val lobsterRequiredLevel = 40

    @Test
    fun `cooking food on a range yields a cooked item and xp`() {
        val player = spawnPlayer("CookOne", 3260, 3218)
        RealWorldHarness.setSkillLevel(player, Skill.COOKING, 99)
        RealWorldHarness.giveItem(player, rawShrimpId, 1)
        spawnObject(rangeObjectId, 3260, 3219)

        RealWorldHarness.itemOnObject(player, rangeObjectId, x = 3260, y = 3219, itemId = rawShrimpId)
        val gotCooked = RealWorldHarness.tickUntil(maxTicks = 20) { player.getInvAmt(cookedShrimpId) > 0 }

        assertTrue(gotCooked, "expected a cooked shrimp within 20 ticks at a very high cooking level")
        assertEquals(0, player.getInvAmt(rawShrimpId), "expected the raw shrimp to be consumed")
        assertTrue(player.getExperience(Skill.COOKING) > 0, "expected cooking xp to be gained")
    }

    @Test
    fun `insufficient level refuses the action`() {
        val player = spawnPlayer("CookLowLevel", 3260, 3221)
        RealWorldHarness.setSkillLevel(player, Skill.COOKING, 1)
        RealWorldHarness.giveItem(player, rawLobsterId, 1)
        spawnObject(rangeObjectId, 3260, 3222)

        RealWorldHarness.itemOnObject(player, rangeObjectId, x = 3260, y = 3222, itemId = rawLobsterId)
        RealWorldHarness.tick(10)

        assertEquals(1, player.getInvAmt(rawLobsterId), "expected the raw lobster not to be consumed on a refused cook")
        assertEquals(0, player.getExperience(Skill.COOKING))
        assertTrue(lobsterRequiredLevel > 1, "sanity check: this recipe must require a higher level than the test player has")
    }
}
