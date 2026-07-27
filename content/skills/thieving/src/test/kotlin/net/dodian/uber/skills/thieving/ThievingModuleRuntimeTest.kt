package net.dodian.uber.skills.thieving

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThievingModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(ThievingModule.descriptor.id, Skill.THIEVING)
        assertEquals(ThievingModule.descriptor.id, ThievingModule.contentManifest.id)
    }

    @Test
    fun `toml targets are loaded`() {
        assertTrue(ThievingModule.targets.isNotEmpty())
        assertTrue(ThievingModule.targets.any { it.name == "man" })
    }
}
