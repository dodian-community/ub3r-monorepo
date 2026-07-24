package net.dodian.uber.skills.slayer

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SlayerModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(SlayerModule.descriptor.id, Skill.SLAYER)
        assertEquals(SlayerModule.descriptor.id, SlayerModule.contentManifest.id)
    }

    @Test
    fun `toml tasks are loaded`() {
        assertTrue(SlayerModule.tasks.isNotEmpty())
        assertTrue(SlayerModule.tasks.any { it.name == "Crawling Hands" })
    }
}
