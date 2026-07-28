package net.dodian.uber.skills.smithing

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

    @Test
    fun `furnace routes use only verified current cache furnaces`() {
        assertEquals(setOf(2030, 3994, 16469), SmithingModule.furnaceObjectIds.toSet())
        assertFalse(SmithingModule.furnaceObjectIds.any { it in setOf(2150, 2151, 2152, 2153, 11666, 29662) })
        SmithingModule.furnaceObjectIds.forEach { objectId ->
            assertTrue(SmithingModule.definition.itemOnObjectBindings.any { objectId in it.objectIds })
        }
        setOf(2030, 16469).forEach { objectId ->
            assertTrue(SmithingModule.definition.objectBindings.any { it.option == 2 && objectId in it.objectIds })
        }
        assertFalse(SmithingModule.definition.objectBindings.any { it.option == 1 && it.objectIds.any { id -> id in SmithingModule.furnaceObjectIds } })
        assertFalse(SmithingModule.definition.objectBindings.any { 3994 in it.objectIds })
    }

    @Test
    fun `anvil routes exclude revision 218 table id`() {
        assertEquals(setOf(2097), SmithingModule.anvilObjectIds.toSet())
        assertFalse(SmithingModule.definition.itemOnObjectBindings.any { 2783 in it.objectIds })
    }
}
