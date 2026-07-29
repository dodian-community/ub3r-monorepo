package net.dodian.uber.skills.crafting

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CraftingModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(CraftingModule.descriptor.id, Skill.CRAFTING)
        assertEquals(CraftingModule.descriptor.id, CraftingModule.contentManifest.id)
    }

    @Test
    fun `toml recipes are loaded`() {
        assertTrue(CraftingModule.gems.isNotEmpty())
        assertTrue(CraftingModule.hides.isNotEmpty())
    }

    @Test
    fun `existing spinning wheels use the Spin option`() {
        setOf(14889, 25824).forEach { objectId ->
            assertTrue(CraftingModule.definition.objectBindings.any { it.option == 2 && objectId in it.objectIds })
        }
        assertTrue(CraftingModule.definition.objectBindings.none { it.option == 1 && it.objectIds.any { id -> id in setOf(14889, 25824) } })
        assertTrue(CraftingModule.definition.objectBindings.none { 4309 in it.objectIds })
    }
}
