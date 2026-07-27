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
}
