package net.dodian.uber.skills.smithing

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SmithingModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(SmithingModule.descriptor.id, Skill.SMITHING)
        assertEquals(SmithingModule.descriptor.id, SmithingModule.contentManifest.id)
    }

    @Test
    fun `toml smelting recipes are loaded`() {
        assertTrue(SmithingModule.smeltingRecipes.isNotEmpty())
        assertTrue(SmithingModule.smeltingRecipes.any { it.barId == 2349 })
    }
}
