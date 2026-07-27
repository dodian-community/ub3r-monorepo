package net.dodian.uber.skills.testkit

import net.dodian.uber.skills.api.SkillRecipe
import net.dodian.uber.skills.runtime.SkillRecipePlanner

/** Deterministic item ledger for module tests; it deliberately has no server or packet dependency. */
class SkillRecipeFixture(items: Map<Int, Int> = emptyMap()) {
    private val amounts = items.toMutableMap()

    fun amount(itemId: Int): Int = amounts[itemId] ?: 0
    fun set(itemId: Int, amount: Int) { amounts[itemId] = amount.coerceAtLeast(0) }
    fun maxCraftable(recipe: SkillRecipe): Int = SkillRecipePlanner.maxCraftable(recipe, ::amount)
}
