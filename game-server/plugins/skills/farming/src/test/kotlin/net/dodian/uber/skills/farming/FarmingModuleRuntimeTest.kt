package net.dodian.uber.skills.farming

import net.dodian.uber.game.api.plugin.skills.SkillItemOnObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectRef
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FarmingModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(FarmingModule.descriptor.id, Skill.FARMING)
        assertEquals(FarmingModule.descriptor.id, FarmingModule.contentManifest.id)
    }

    @Test
    fun `toml crops are loaded`() {
        assertTrue(FarmingModule.crops.isNotEmpty())
        assertTrue(FarmingModule.crops.any { it.name == "potato" })
        assertEquals(44, FarmingModule.crops.size)
    }

    @Test
    fun `revision 218 empty farming patch roots remain explicitly owned`() {
        val expected = setOf(8573, 7840, 8132)
        assertEquals(expected, FarmingModule.patchObjectIds.toSet())
        expected.forEach { objectId ->
            assertTrue(FarmingModule.definition.objectBindings.any { it.option == 2 && objectId in it.objectIds })
            assertTrue(FarmingModule.definition.itemOnObjectBindings.any { objectId in it.objectIds })
        }
    }

    @Test
    fun `patch click routes to SkillFarming clickPatch for live patch locations`() {
        val binding = FarmingModule.definition.objectBindings.single { it.option == 1 && 8552 in it.objectIds }
        val player = FakeSkillPlayer()
        val target = SkillObjectRef(8552, SkillPosition(2809, 3463, 0))

        val handled = binding.handler(SkillObjectInteraction(player, 1, target))

        assertTrue(handled)
        assertEquals(listOf(8552), player.farmingClickPatchCalls)
    }

    @Test
    fun `seed item-on-patch routes to SkillFarming useItem`() {
        val binding = FarmingModule.definition.itemOnObjectBindings.single { 8552 in it.objectIds && 5318 in it.itemIds }
        val player = FakeSkillPlayer()
        val target = SkillObjectRef(8552, SkillPosition(2809, 3463, 0))

        val handled = binding.handler(SkillItemOnObjectInteraction(player, target, 5318, 0, -1))

        assertTrue(handled)
        assertEquals(listOf(8552 to 5318), player.farmingUseItemCalls)
    }

    @Test
    fun `compost bin open and dump options route to SkillFarming interactBin`() {
        val openBinding = FarmingModule.definition.objectBindings.single { it.option == 1 && 7837 in it.objectIds }
        val dumpBinding = FarmingModule.definition.objectBindings.single { it.option == 5 && 7837 in it.objectIds }
        val player = FakeSkillPlayer()
        val target = SkillObjectRef(7837, SkillPosition(2804, 3464, 0))

        openBinding.handler(SkillObjectInteraction(player, 1, target))
        dumpBinding.handler(SkillObjectInteraction(player, 5, target))

        assertEquals(listOf(7837 to 1, 7837 to 5), player.farmingInteractBinCalls)
    }
}
