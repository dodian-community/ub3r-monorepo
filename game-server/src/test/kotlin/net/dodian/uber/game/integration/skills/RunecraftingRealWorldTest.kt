package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Runecrafting's altar
 * object-click flow, driving the same packet-equivalent [RealWorldHarness.objectOption]
 * entry point a real client uses. Unlike the gathering skills, an altar click crafts all
 * carried essence in one synchronous step rather than looping over ticks. Complements
 * (does not replace) the module-level RunecraftingModuleRuntimeTest.
 *
 * Ids sourced from plugins/skills/runecrafting/src/main/resources/runecrafting/altars.toml
 * and RunecraftingModule.RUNE_ESSENCE_ID. The bonus-rune roll is real, unseeded RNG
 * (player.random.chance) - only the deterministic minimum (>= essence count) and the xp
 * (fixed per essence, unaffected by the bonus roll) are asserted, not the exact rune count.
 */
class RunecraftingRealWorldTest : RealWorldSkillTest() {
    private val runeEssenceId = 1436
    private val airAltarObjectId = 34768
    private val airRuneId = 561
    private val airExperiencePerEssence = 60

    private val bodyAltarObjectId = 27978
    private val bodyRuneId = 565
    private val bodyRequiredLevel = 50

    @Test
    fun `crafting runes at an altar consumes essence and yields runes plus xp`() {
        val player = spawnPlayer("RunecrafterOne", 3310, 3218)
        RealWorldHarness.setSkillLevel(player, Skill.RUNECRAFTING, 1)
        val essenceGiven = 5
        RealWorldHarness.giveItem(player, runeEssenceId, essenceGiven)
        spawnObject(airAltarObjectId, 3310, 3219)

        RealWorldHarness.objectOption(player, airAltarObjectId, option = 1, x = 3310, y = 3219)
        val gotRunes = RealWorldHarness.tickUntil(maxTicks = 10) { player.getInvAmt(airRuneId) > 0 }

        assertTrue(gotRunes, "expected air runes within 10 ticks")
        assertEquals(0, player.getInvAmt(runeEssenceId), "expected all rune essence to be consumed")
        assertTrue(player.getInvAmt(airRuneId) >= essenceGiven, "expected at least one rune per essence crafted")
        assertEquals(
            airExperiencePerEssence * essenceGiven,
            player.getExperience(Skill.RUNECRAFTING),
            "expected xp = experiencePerEssence * essence count, independent of the bonus-rune roll",
        )
    }

    @Test
    fun `insufficient level refuses the craft`() {
        val player = spawnPlayer("RunecrafterLowLevel", 3310, 3221)
        RealWorldHarness.setSkillLevel(player, Skill.RUNECRAFTING, 1)
        RealWorldHarness.giveItem(player, runeEssenceId, 5)
        spawnObject(bodyAltarObjectId, 3310, 3222)

        RealWorldHarness.objectOption(player, bodyAltarObjectId, option = 1, x = 3310, y = 3222)
        RealWorldHarness.tick(5)

        assertEquals(0, player.getInvAmt(bodyRuneId))
        assertEquals(5, player.getInvAmt(runeEssenceId), "expected essence not to be consumed on a refused craft")
        assertEquals(0, player.getExperience(Skill.RUNECRAFTING))
        assertTrue(bodyRequiredLevel > 1, "sanity check: this altar must require a higher level than the test player has")
    }
}
