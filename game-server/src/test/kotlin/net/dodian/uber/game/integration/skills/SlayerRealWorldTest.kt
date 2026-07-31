package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Slayer - the helm-assembly flow
 * (item-on-item like Crafting's gem-cutting, executing directly with no menu) and the
 * master-conversation task-assignment flow (a branching SkillUi.dialogue tree, driven via
 * [RealWorldHarness.npcOption]/[RealWorldHarness.continueDialogue]/[RealWorldHarness.button]).
 * Complements (does not replace) the module-level SlayerModuleRuntimeTest.
 *
 * Ids sourced from SlayerModule.assembleSlayerHelm and SlayerModule's master npcClick bindings.
 */
class SlayerRealWorldTest : RealWorldSkillTest() {
    private val helmComponents = intArrayOf(4155, 4156, 4164, 4166, 4168, 4551, 6720, 8923)
    private val blackMaskId = 8921
    private val assembledHelmId = 11864

    @Test
    fun `assembling a slayer helm consumes all components and the black mask`() {
        val player = spawnPlayer("SlayerOne", 3330, 3218)
        RealWorldHarness.setSkillLevel(player, Skill.CRAFTING, 70)
        helmComponents.forEach { RealWorldHarness.giveItem(player, it, 1) }
        RealWorldHarness.giveItem(player, blackMaskId, 1)

        RealWorldHarness.itemOnItem(player, helmComponents[0], helmComponents[1])
        RealWorldHarness.tick(3)

        assertTrue(player.getInvAmt(assembledHelmId) > 0, "expected an assembled slayer helm")
        helmComponents.forEach { assertEquals(0, player.getInvAmt(it), "expected component $it to be consumed") }
        assertEquals(0, player.getInvAmt(blackMaskId), "expected the black mask to be consumed")
    }

    @Test
    fun `insufficient crafting level refuses assembly`() {
        val player = spawnPlayer("SlayerLowLevel", 3330, 3221)
        RealWorldHarness.setSkillLevel(player, Skill.CRAFTING, 1)
        helmComponents.forEach { RealWorldHarness.giveItem(player, it, 1) }
        RealWorldHarness.giveItem(player, blackMaskId, 1)

        RealWorldHarness.itemOnItem(player, helmComponents[0], helmComponents[1])
        RealWorldHarness.tick(3)

        assertEquals(0, player.getInvAmt(assembledHelmId))
        helmComponents.forEach { assertEquals(1, player.getInvAmt(it), "expected component $it not to be consumed on a refused assembly") }
    }

    @Test
    fun `talking to mazchna, saying yes, and requesting a task assigns one`() {
        val player = spawnPlayer("SlayerTalker", 2884, 3450)
        val mazchna = spawnNpc(402, 2885, 3450)
        RealWorldHarness.setSkillLevel(player, Skill.SLAYER, 10)

        RealWorldHarness.npcOption(player, mazchna, 1) // talk-to
        RealWorldHarness.tick(5)
        RealWorldHarness.continueDialogue(player) // past "Need help with your slayer assignment?"
        RealWorldHarness.button(player, 2461) // "Yes please."
        RealWorldHarness.button(player, 2461) // "I'd like a task please"

        assertTrue(player.slayerTaskState.remainingAmount > 0, "expected a task to be assigned")
        assertEquals(402, player.slayerTaskState.masterNpcId)
    }
}
