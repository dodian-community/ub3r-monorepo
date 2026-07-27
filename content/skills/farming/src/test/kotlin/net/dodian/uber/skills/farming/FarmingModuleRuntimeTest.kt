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
    fun `transform root farming patch ids remain explicitly owned`() {
        val expected = setOf(8552, 8553, 7848, 8151, 7965, 8554, 8555, 7849, 8152, 7580, 8556, 8557, 7850, 8153, 19147, 7962, 8388)
        assertEquals(expected, FarmingModule.patchObjectIds.toSet())
        expected.forEach { objectId ->
            assertTrue(FarmingModule.definition.objectBindings.any { it.option == 1 && objectId in it.objectIds })
            assertTrue(FarmingModule.definition.itemOnObjectBindings.any { objectId in it.objectIds })
        }
    }
}
