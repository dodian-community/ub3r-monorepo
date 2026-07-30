package net.dodian.uber.game.engine.systems.skills.skillguide

import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TomlSkillGuideLoaderTest {

    @Test
    fun testLoadAllGuides() {
        val definitions = TomlSkillGuideLoader.load()
        assertNotNull(definitions)
        assertFalse(definitions.isEmpty(), "Definitions should not be empty")

        // We expect guide definitions for all 21 skills
        val expectedSkills = Skill.values()
        for (skill in expectedSkills) {
            val definition = definitions[skill.id]
            assertNotNull(definition, "Missing guide definition for skill: ${skill.name}")
            assertFalse(definition!!.tabLabels.isEmpty(), "Tab labels should not be empty for skill: ${skill.name}")
        }
    }

    @Test
    fun testAttackGuideDetails() {
        val definitions = TomlSkillGuideLoader.load()
        val attackGuide = definitions[Skill.ATTACK.id]
        assertNotNull(attackGuide)

        // Check tabs
        val tabs = attackGuide!!.tabLabels
        assertEquals(4, tabs.size)
        assertEquals("Attack", tabs[0].text)
        assertEquals("Defence", tabs[1].text)
        assertEquals("Range", tabs[2].text)
        assertEquals("Magic", tabs[3].text)

        // Check page entries
        val mockClient = net.dodian.uber.game.model.entity.player.Client(null, 1)
        val page = attackGuide.pageProvider(mockClient, 0)
        assertNotNull(page)
        assertFalse(page!!.entries.isEmpty())

        val whip = page.entries.first()
        assertEquals("Abyssal Whip", whip.text)
        assertEquals("1", whip.levelText)
        assertEquals(4151, whip.itemId)
    }

    @Test
    fun testLayoutShowHide() {
        val definitions = TomlSkillGuideLoader.load()
        
        // Strength guide layout
        val strengthGuide = definitions[Skill.STRENGTH.id]
        assertNotNull(strengthGuide)
        val strengthLayout = strengthGuide!!.layout
        assertTrue(strengthLayout.hideComponents.contains(8825))
        assertTrue(strengthLayout.hideComponents.contains(8813))

        // Crafting guide layout
        val craftingGuide = definitions[Skill.CRAFTING.id]
        assertNotNull(craftingGuide)
        val craftingLayout = craftingGuide!!.layout
        assertTrue(craftingLayout.showComponents.contains(8827))
        assertTrue(craftingLayout.showComponents.contains(8828))
        assertTrue(craftingLayout.showComponents.contains(8838))
        assertTrue(craftingLayout.hideComponents.contains(8841))
        assertTrue(craftingLayout.hideComponents.contains(8850))
    }
}
