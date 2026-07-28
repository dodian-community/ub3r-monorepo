package net.dodian.uber.skills.runtime

import net.dodian.uber.skills.api.skillRecipe
import net.dodian.uber.skills.api.SkillMultiConfig
import net.dodian.uber.skills.api.SkillMultiEntry
import net.dodian.uber.skills.api.SkillMultiSelection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SkillRecipePlannerTest {
    @Test
    fun `limits production by the scarcest material`() {
        val recipe = skillRecipe(100) { material(1, 2); material(2, 3) }
        assertEquals(2, SkillRecipePlanner.maxCraftable(recipe) { if (it == 1) 9 else 8 })
    }

    @Test
    fun `selection is revalidated and clamped against current inventory`() {
        val recipe = skillRecipe("smithing.bronze-bar", 2349) { material(436); material(438) }
        val config = SkillMultiConfig("smithing.furnace", entries = listOf(SkillMultiEntry(recipe)))
        val selection = SkillMultiSelection(config.key, recipe.key, 20)
        val resolved = SkillRecipePlanner.resolve(config, selection) { if (it == 436) 4 else 2 }
        assertEquals(recipe to 2, resolved)
        assertEquals(null, SkillRecipePlanner.resolve(config, selection) { 0 })
    }
}
