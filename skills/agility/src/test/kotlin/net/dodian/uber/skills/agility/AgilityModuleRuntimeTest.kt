package net.dodian.uber.skills.agility

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AgilityModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(AgilityModule.descriptor.id, Skill.AGILITY)
        assertEquals(AgilityModule.descriptor.id, AgilityModule.contentManifest.id)
    }

    @Test
    fun `toml obstacles are loaded`() {
        assertTrue(AgilityModule.obstacles.isNotEmpty())
        assertTrue(AgilityModule.obstacles.any { it.name == "log_balance" })
    }
}
