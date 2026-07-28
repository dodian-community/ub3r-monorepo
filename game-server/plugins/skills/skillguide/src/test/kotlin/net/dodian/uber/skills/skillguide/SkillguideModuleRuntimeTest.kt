package net.dodian.uber.skills.skillguide

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkillguideModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        assertEquals(SkillguideModule.descriptor.id, SkillguideModule.contentManifest.id)
    }

    @Test
    fun `toml guides are loaded`() {
        assertTrue(SkillguideModule.guides.isNotEmpty())
        assertTrue(SkillguideModule.guides.any { it.skillName == "Attack" })
    }

    @Test
    fun `plugin owns guide book every skill button and every subtab`() {
        val definition = SkillguideModule.definition
        assertEquals(listOf(1856), definition.itemBindings.single().itemIds.toList())
        assertEquals(29, definition.buttonBindings.size)
        assertTrue(definition.buttonBindings.all { it.requiredInterfaceId == 8714 })
        assertTrue(definition.buttonBindings.any { 95068 in it.rawButtonIds })
        assertTrue(definition.buttonBindings.any { 34155 in it.rawButtonIds })
    }
}
