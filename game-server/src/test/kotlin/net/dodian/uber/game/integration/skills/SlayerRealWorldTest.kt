package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Slayer's helm-assembly flow -
 * item-on-item like Crafting's gem-cutting, executing directly with no menu. Task
 * assignment/kill-count is intentionally NOT covered here: SlayerModule's own comments
 * say master-conversation task assignment "remain[s] on the legacy route until the
 * complete 34-task dialogue/state machine is transferred," so there's no clean-API entry
 * point yet to drive it through. Complements (does not replace) the module-level
 * SlayerModuleRuntimeTest.
 *
 * Ids sourced from SlayerModule.assembleSlayerHelm.
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
}
