package net.dodian.uber.skills.skillguide

import net.dodian.uber.game.api.plugin.skills.SkillButtonInteraction
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkillguideModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        assertEquals(SkillguideModule.descriptor.id, SkillguideModule.contentManifest.id)
    }

    @Test
    fun `plugin owns guide book every skill button and every subtab`() {
        val definition = SkillguideModule.definition
        assertEquals(listOf(1856), definition.itemBindings.single().itemIds.toList())
        assertEquals(29, definition.buttonBindings.size)
        assertEquals(21, definition.buttonBindings.count { it.requiredInterfaceId == -1 })
        assertEquals(8, definition.buttonBindings.count { it.requiredInterfaceId == 8714 })
        assertTrue(definition.buttonBindings.any { 13928 in it.rawButtonIds })
        assertTrue(definition.buttonBindings.any { 34155 in it.rawButtonIds })
    }

    @Test
    fun `side-tab skill button opens that skill's guide at tab 0`() {
        val binding = SkillguideModule.definition.buttonBindings.single { 8658 in it.rawButtonIds }
        val player = FakeSkillPlayer()

        val handled = binding.handler(SkillButtonInteraction(player, 8658, 0, -1))

        assertTrue(handled)
        assertEquals(listOf(Skill.AGILITY.id to 0), player.skillGuideCalls)
    }

    @Test
    fun `sub-tab routes to the currently-open skill instead of a module-local copy`() {
        val subTabBinding = SkillguideModule.definition.buttonBindings.single { it.requiredInterfaceId == 8714 && 8824 in it.rawButtonIds }
        val player = FakeSkillPlayer()
        // Simulate the player already viewing Herblore's guide (as SkillGuide.open() would have
        // recorded via the real contentRuntimeState field this bridges to).
        player.currentSkillGuideSkillIdValue = Skill.HERBLORE.id

        val handled = subTabBinding.handler(SkillButtonInteraction(player, 8824, 0, 8714))

        assertTrue(handled)
        assertEquals(listOf(Skill.HERBLORE.id to 2), player.skillGuideCalls)
    }
}
