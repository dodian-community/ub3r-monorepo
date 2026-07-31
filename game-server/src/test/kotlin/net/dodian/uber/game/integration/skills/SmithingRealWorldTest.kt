package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Smithing's furnace-smelting
 * flow, driving the same packet-equivalent [RealWorldHarness.itemOnObject] entry point a
 * real client uses and advancing real game ticks. Complements (does not replace) the
 * module-level SmithingModuleRuntimeTest, which exercises the same module against
 * FakeSkillPlayer.
 *
 * Furnace/ore/bar ids sourced from SmithingModule.furnaceObjectIds and
 * plugins/skills/smithing/src/main/resources/smithing/recipes.toml. Anvil "make-x"
 * (bar -> finished item) coverage is deferred - see docs/development follow-up notes;
 * this file covers the furnace/smelting leg only.
 */
class SmithingRealWorldTest : RealWorldSkillTest() {
    private val furnaceId = 2030
    private val copperOreId = 436
    private val tinOreId = 438
    private val bronzeBarId = 2349

    private val ironOreId = 440
    private val ironBarLevel = 15

    @Test
    fun `smelting a bronze bar consumes both ores and yields a bar plus xp`() {
        val player = spawnPlayer("SmithOne", 3240, 3218)
        RealWorldHarness.setSkillLevel(player, Skill.SMITHING, 1)
        RealWorldHarness.giveItem(player, copperOreId, 1)
        RealWorldHarness.giveItem(player, tinOreId, 1)
        spawnObject(furnaceId, 3240, 3219)

        RealWorldHarness.itemOnObject(player, furnaceId, x = 3240, y = 3219, itemId = copperOreId)
        val gotBar = RealWorldHarness.tickUntil(maxTicks = 20) { player.getInvAmt(bronzeBarId) > 0 }

        assertTrue(gotBar, "expected a bronze bar within 20 ticks")
        assertEquals(0, player.getInvAmt(copperOreId), "expected copper ore to be consumed")
        assertEquals(0, player.getInvAmt(tinOreId), "expected tin ore to be consumed")
        assertTrue(player.getExperience(Skill.SMITHING) > 0, "expected smithing xp to be gained")
    }

    @Test
    fun `insufficient level refuses the smelt`() {
        val player = spawnPlayer("SmithLowLevel", 3240, 3221)
        RealWorldHarness.setSkillLevel(player, Skill.SMITHING, 1)
        RealWorldHarness.giveItem(player, ironOreId, 1)
        spawnObject(furnaceId, 3240, 3222)

        RealWorldHarness.itemOnObject(player, furnaceId, x = 3240, y = 3222, itemId = ironOreId)
        RealWorldHarness.tick(10)

        assertEquals(1, player.getInvAmt(ironOreId), "expected the ore not to be consumed on a refused smelt")
        assertEquals(0, player.getExperience(Skill.SMITHING))
        assertTrue(ironBarLevel > 1, "sanity check: this recipe must require a higher level than the test player has")
    }

    @Test
    fun `missing one of the required ores refuses the smelt and rolls back the transaction`() {
        val player = spawnPlayer("SmithMissingOre", 3240, 3224)
        RealWorldHarness.setSkillLevel(player, Skill.SMITHING, 1)
        RealWorldHarness.giveItem(player, copperOreId, 1)
        spawnObject(furnaceId, 3240, 3225)

        RealWorldHarness.itemOnObject(player, furnaceId, x = 3240, y = 3225, itemId = copperOreId)
        RealWorldHarness.tick(10)

        assertEquals(0, player.getInvAmt(bronzeBarId))
        assertEquals(1, player.getInvAmt(copperOreId), "expected the copper ore already held to be untouched by the rolled-back transaction")
        assertEquals(0, player.getExperience(Skill.SMITHING))
    }

    @Test
    fun `dragonfire shield assembles from a draconic visage used on the shield npc`() {
        val player = spawnPlayer("SmithDfs", 3240, 3227)
        RealWorldHarness.giveItem(player, 1540, 1)
        RealWorldHarness.giveItem(player, 11286, 1)
        RealWorldHarness.giveItem(player, 995, 1_500_000)
        val npc = spawnNpc(535, 3241, 3227)

        RealWorldHarness.itemOnNpc(player, npc, 1540)
        RealWorldHarness.tick(5)

        assertEquals(1, player.getInvAmt(11284), "expected an assembled dragonfire shield")
        assertEquals(0, player.getInvAmt(1540))
        assertEquals(0, player.getInvAmt(11286))
        assertEquals(0, player.getInvAmt(995))
    }

    @Test
    fun `rockshell item pair opens the menu and a button click crafts the piece`() {
        val player = spawnPlayer("SmithRockshell", 3240, 3230)
        RealWorldHarness.setSkillLevel(player, Skill.SMITHING, 60)
        RealWorldHarness.giveItem(player, 2347, 1)
        RealWorldHarness.giveItem(player, 6159, 1)
        RealWorldHarness.giveItem(player, 6161, 1)

        RealWorldHarness.itemOnItem(player, 6159, 6161)
        RealWorldHarness.dialogueOption(player, 1)

        assertEquals(1, player.getInvAmt(6128), "expected a Rock-shell head")
        assertEquals(0, player.getInvAmt(6159))
        assertEquals(0, player.getInvAmt(6161))
    }

    @Test
    fun `superheat spell smelts a bronze bar without visiting a furnace`() {
        val player = spawnPlayer("SmithSuperheat", 3240, 3233)
        RealWorldHarness.setSkillLevel(player, Skill.SMITHING, 1)
        RealWorldHarness.setSkillLevel(player, Skill.MAGIC, 43)
        RealWorldHarness.giveItem(player, copperOreId, 1)
        RealWorldHarness.giveItem(player, tinOreId, 1)
        RealWorldHarness.giveItem(player, 561, 1)
        RealWorldHarness.tick(5)

        RealWorldHarness.magicOnItem(player, copperOreId, 1173)

        assertEquals(1, player.getInvAmt(bronzeBarId))
        assertEquals(0, player.getInvAmt(copperOreId))
        assertEquals(0, player.getInvAmt(tinOreId))
        assertEquals(0, player.getInvAmt(561))
        assertTrue(player.getExperience(Skill.SMITHING) > 0)
        assertTrue(player.getExperience(Skill.MAGIC) > 0)
    }
}
