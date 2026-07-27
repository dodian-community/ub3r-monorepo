package net.dodian.uber.skills.agility

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AgilityModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(AgilityModule.descriptor.id, Skill.AGILITY)
        assertEquals(AgilityModule.descriptor.id, AgilityModule.contentManifest.id)
    }

    @Test
    fun `per-course toml obstacles are loaded correctly`() {
        assertTrue(AgilityModule.obstacles.isNotEmpty())

        val courses = AgilityModule.obstacles.map { it.course }.toSet()
        assertTrue(courses.contains("gnome"))
        assertTrue(courses.contains("barbarian"))
        assertTrue(courses.contains("wilderness"))
        assertTrue(courses.contains("werewolf"))
        assertTrue(courses.contains("shortcuts"))
    }

    @Test
    fun `gnome course log balance uses walk movement with balance animation`() {
        val gnomeLog = AgilityModule.obstacles.firstOrNull { it.course == "gnome" && it.name == "log_balance" }
        assertNotNull(gnomeLog)
        assertEquals(AgilityMovementType.WALK, gnomeLog!!.movementType)
        assertEquals(762, gnomeLog.animationId)
        assertEquals(23145, gnomeLog.objectId)
    }

    @Test
    fun `gnome course pipe defines lap completion and agility tickets`() {
        val gnomePipe = AgilityModule.obstacles.firstOrNull { it.course == "gnome" && it.name == "pipe_one" }
        assertNotNull(gnomePipe)
        assertTrue(gnomePipe!!.isLapCompletion)
        assertEquals(1, gnomePipe.ticketBase)
        assertEquals(11, gnomePipe.ticketScaleDivisor)
        assertEquals(1050, gnomePipe.bonusExperience)
        assertEquals("You finished a gnome lap!", gnomePipe.lapMessage)
    }
}
