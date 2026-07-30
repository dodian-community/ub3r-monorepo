package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Mining, driving the same
 * packet-equivalent [RealWorldHarness.objectOption] entry point a real client uses and
 * advancing real game ticks. Complements (does not replace) the module-level
 * MiningModuleRuntimeTest, which exercises the same module against FakeSkillPlayer.
 *
 * Ids sourced from plugins/skills/mining/src/main/resources/mining/{rocks,pickaxes}.toml.
 * No deterministic RNG seam exists in this engine (Randoms.kt calls Math.random()
 * directly) - gem-drop chance is intentionally not asserted anywhere in this file.
 */
class MiningRealWorldTest : RealWorldSkillTest() {
    private val copperRockId = 10943
    private val copperOreId = 436
    private val bronzePickaxeId = 1265

    private val ironRockId = 11364
    private val ironRequiredLevel = 15

    @Test
    fun `mining a rock with a pickaxe yields ore and xp`() {
        val player = spawnPlayer("MinerOne", 3230, 3218)
        RealWorldHarness.setSkillLevel(player, Skill.MINING, 1)
        RealWorldHarness.giveItem(player, bronzePickaxeId, 1)
        spawnObject(copperRockId, 3230, 3219)

        RealWorldHarness.objectOption(player, copperRockId, option = 1, x = 3230, y = 3219)
        val gotOre = RealWorldHarness.tickUntil(maxTicks = 50) { player.getInvAmt(copperOreId) > 0 }

        assertTrue(gotOre, "expected at least one ore within 50 ticks")
        assertTrue(player.getExperience(Skill.MINING) > 0, "expected mining xp to be gained")
    }

    @Test
    fun `missing a pickaxe refuses the action`() {
        val player = spawnPlayer("MinerNoPickaxe", 3230, 3221)
        RealWorldHarness.setSkillLevel(player, Skill.MINING, 1)
        spawnObject(copperRockId, 3230, 3222)

        RealWorldHarness.objectOption(player, copperRockId, option = 1, x = 3230, y = 3222)
        RealWorldHarness.tick(10)

        assertEquals(0, player.getInvAmt(copperOreId))
        assertEquals(0, player.getExperience(Skill.MINING))
    }

    @Test
    fun `insufficient level refuses the action`() {
        val player = spawnPlayer("MinerLowLevel", 3230, 3224)
        RealWorldHarness.setSkillLevel(player, Skill.MINING, 1)
        RealWorldHarness.giveItem(player, bronzePickaxeId, 1)
        spawnObject(ironRockId, 3230, 3225)

        RealWorldHarness.objectOption(player, ironRockId, option = 1, x = 3230, y = 3225)
        RealWorldHarness.tick(10)

        assertEquals(0, player.getExperience(Skill.MINING))
        assertTrue(ironRequiredLevel > 1, "sanity check: this rock must require a higher level than the test player has")
    }

    /**
     * This ruleset's rocks never deplete (no depletion/respawn state exists in
     * GatheringPipeline/MiningModule), unlike vanilla OSRS, and the rest-policy stop is
     * probabilistic (no RNG seam to force it deterministically) - so neither is a
     * reliable real-world assertion here. A full inventory is this codebase's real,
     * deterministic substitute: GatheringSpotBuilder.requireFreeInventory is
     * re-validated every cycle, so mining stops on its own the instant there's no
     * space left, with no RNG involved.
     */
    @Test
    fun `mining stops on its own once the inventory is full`() {
        val player = spawnPlayer("MinerFullInventory", 3230, 3227)
        RealWorldHarness.setSkillLevel(player, Skill.MINING, 1)
        RealWorldHarness.giveItem(player, bronzePickaxeId, 1)
        val freeSlotsToLeave = 2
        RealWorldHarness.giveItem(player, copperOreId, 28 - freeSlotsToLeave - 1)
        assertEquals(freeSlotsToLeave, player.freeSlots(), "test setup: expected exactly $freeSlotsToLeave free slots before mining")
        spawnObject(copperRockId, 3230, 3228)

        RealWorldHarness.objectOption(player, copperRockId, option = 1, x = 3230, y = 3228)
        val filledUp = RealWorldHarness.tickUntil(maxTicks = 50) { player.freeSlots() == 0 }
        assertTrue(filledUp, "expected mining to fill the remaining $freeSlotsToLeave free slots within 50 ticks")
        val oreWhenFull = player.getInvAmt(copperOreId)

        RealWorldHarness.tick(15)

        assertEquals(oreWhenFull, player.getInvAmt(copperOreId), "expected mining to stop once the inventory had no free slots")
    }
}
