package net.dodian.uber.skills.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SkillModuleTest {
    @Test
    fun `recipe DSL consolidates a repeated material`() {
        val recipe = skillRecipe(200) {
            output(3)
            material(100, 2)
            material(100, 4)
        }

        assertEquals(3, recipe.outputAmount)
        assertEquals(listOf(SkillMaterial(100, 6)), recipe.materials)
    }

    @Test
    fun `module ids are stable and validated`() {
        assertEquals("skill.woodcutting", SkillModuleDescriptor("skill.woodcutting", "Woodcutting").id)
        assertThrows(IllegalArgumentException::class.java) {
            SkillModuleDescriptor("Woodcutting", "Woodcutting")
        }
    }

    @Test
    fun `skill multi validates unique entries and active client capacity`() {
        val first = skillRecipe("cooking.shrimp", 315) { material(317) }
        val second = skillRecipe("cooking.meat", 2142) { material(2132) }
        val config = SkillMultiConfig(
            key = "cooking.range",
            verb = "cook",
            action = SkillMultiAction.COOK,
            entries = listOf(SkillMultiEntry(first), SkillMultiEntry(second)),
        )
        assertEquals("What would you like to cook?", config.title)
        assertThrows(IllegalArgumentException::class.java) {
            SkillMultiConfig("duplicate", entries = listOf(SkillMultiEntry(first), SkillMultiEntry(first)))
        }
    }
}
