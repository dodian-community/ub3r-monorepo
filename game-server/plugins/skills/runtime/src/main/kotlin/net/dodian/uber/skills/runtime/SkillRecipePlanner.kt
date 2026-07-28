package net.dodian.uber.skills.runtime

import net.dodian.uber.skills.api.SkillRecipe
import net.dodian.uber.skills.api.SkillMultiConfig
import net.dodian.uber.skills.api.SkillMultiEntry
import net.dodian.uber.skills.api.SkillMultiSelection

/** Pure recipe planning equivalent to OpenRune's skill-multi availability calculation. */
object SkillRecipePlanner {
    fun maxCraftable(recipe: SkillRecipe, itemAmounts: (Int) -> Int): Int {
        if (recipe.materials.isEmpty()) return 0
        return recipe.materials.minOf { material ->
            (itemAmounts(material.itemId).coerceAtLeast(0) / material.amount).coerceAtLeast(0)
        }
    }

    fun available(recipes: Iterable<SkillRecipe>, itemAmounts: (Int) -> Int): List<Pair<SkillRecipe, Int>> =
        recipes.mapNotNull { recipe -> maxCraftable(recipe, itemAmounts).takeIf { it > 0 }?.let { recipe to it } }

    fun available(config: SkillMultiConfig, itemAmounts: (Int) -> Int): List<Pair<SkillMultiEntry, Int>> =
        config.entries.mapNotNull { entry ->
            maxCraftable(entry.recipe, itemAmounts).takeIf { it > 0 }?.let { entry to it }
        }

    fun resolve(
        config: SkillMultiConfig,
        selection: SkillMultiSelection,
        itemAmounts: (Int) -> Int,
    ): Pair<SkillRecipe, Int>? {
        if (selection.configKey != config.key) return null
        val recipe = config.entries.singleOrNull { it.recipe.key == selection.recipeKey }?.recipe ?: return null
        val amount = selection.amount.coerceAtMost(maxCraftable(recipe, itemAmounts))
        return if (amount > 0) recipe to amount else null
    }
}
