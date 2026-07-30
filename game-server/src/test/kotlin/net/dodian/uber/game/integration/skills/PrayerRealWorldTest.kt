package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Prayer: burying bones
 * (itemClick), restoring prayer points at an altar (objectClick), and offering bones at
 * an offering-enabled altar (itemOnObject, a real productionAction cycle). This module
 * has no level gates on any of these three actions, so unlike the other skill test files
 * there are no insufficient-level refusal cases here. Complements (does not replace) the
 * module-level PrayerModuleRuntimeTest.
 *
 * Ids sourced from plugins/skills/prayer/src/main/resources/prayer/{bones,altars}.toml.
 */
class PrayerRealWorldTest : RealWorldSkillTest() {
    private val normalBonesId = 526
    private val normalBonesXp = 45

    private val mainAltarObjectId = 409 // offering_enabled = true
    private val restorationAltarObjectId = 20377 // offering_enabled = false

    @Test
    fun `burying a bone consumes it and yields xp`() {
        val player = spawnPlayer("PrayerBurier", 3340, 3218)
        RealWorldHarness.giveItem(player, normalBonesId, 1)

        val handled = RealWorldHarness.itemClick(player, normalBonesId, option = 1)
        RealWorldHarness.tick(2)

        assertTrue(handled, "expected the bury click to be handled")
        assertEquals(0, player.getInvAmt(normalBonesId), "expected the bone to be consumed")
        assertEquals(normalBonesXp, player.getExperience(Skill.PRAYER))
    }

    @Test
    fun `clicking a restoration altar restores prayer points to maximum`() {
        val player = spawnPlayer("PrayerRestorer", 3340, 3221)
        player.currentPrayer = 0
        spawnObject(restorationAltarObjectId, 3340, 3222)

        RealWorldHarness.objectOption(player, restorationAltarObjectId, option = 1, x = 3340, y = 3222)
        RealWorldHarness.tick(3)

        assertEquals(player.maxPrayer, player.currentPrayer, "expected prayer points restored to maximum")
    }

    @Test
    fun `offering a bone at an altar consumes it and yields multiplied xp`() {
        val player = spawnPlayer("PrayerOfferer", 3340, 3224)
        RealWorldHarness.setSkillLevel(player, Skill.FIREMAKING, 1)
        RealWorldHarness.giveItem(player, normalBonesId, 1)
        spawnObject(mainAltarObjectId, 3340, 3225)

        RealWorldHarness.itemOnObject(player, mainAltarObjectId, x = 3340, y = 3225, itemId = normalBonesId)
        val gotXp = RealWorldHarness.tickUntil(maxTicks = 20) { player.getExperience(Skill.PRAYER) > 0 }

        val expectedMultiplier = 2.0 + (1 + 1).toDouble() / 100
        val expectedXp = (normalBonesXp * expectedMultiplier).toInt()
        assertTrue(gotXp, "expected prayer xp within 20 ticks")
        assertEquals(0, player.getInvAmt(normalBonesId), "expected the bone to be consumed")
        assertEquals(expectedXp, player.getExperience(Skill.PRAYER), "expected xp = bone xp * (2.0 + (firemaking level + 1) / 100)")
    }
}
