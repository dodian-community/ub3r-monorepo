package net.dodian.uber.skills.farming

import net.dodian.uber.game.model.player.skills.Skill
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
}
