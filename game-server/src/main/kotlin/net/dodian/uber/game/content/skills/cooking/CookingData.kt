package net.dodian.uber.game.content.skills.cooking

import net.dodian.uber.game.systems.skills.SkillActionRequest
import net.dodian.uber.game.systems.skills.SkillActionState

data class CookingDefinition(
    val rawItemId: Int,
    val cookedItemId: Int,
    val burntItemId: Int,
    val experience: Int,
    val requiredLevel: Int,
    val burnRollBase: Int,
)

data class CookingRequest(
    val itemId: Int,
    val cookIndex: Int,
    val amount: Int,
) : SkillActionRequest

data class CookingState(
    val itemId: Int,
    val cookIndex: Int,
    val remaining: Int,
) : SkillActionState

object CookingData {
    @JvmField
    val recipes: List<CookingDefinition> = listOf(
        CookingDefinition(317, 315, 323, 150, 1, 30),
        CookingDefinition(2134, 2142, 2146, 100, 1, 30),
        CookingDefinition(2132, 2142, 2146, 100, 1, 30),
        CookingDefinition(2138, 2140, 2144, 50, 1, 36),
        CookingDefinition(2307, 2309, 2311, 170, 10, 36),
        CookingDefinition(3363, 3369, 3375, 200, 15, 42),
        CookingDefinition(335, 333, 343, 250, 20, 50),
        CookingDefinition(331, 329, 343, 350, 30, 60),
        CookingDefinition(377, 379, 381, 500, 40, 70),
        CookingDefinition(371, 373, 375, 720, 50, 80),
        CookingDefinition(7944, 7946, 7948, 870, 60, 90),
        CookingDefinition(383, 385, 387, 1220, 70, 100),
        CookingDefinition(395, 397, 399, 1600, 85, 110),
        CookingDefinition(389, 391, 393, 2100, 95, 120),
    )

    @JvmStatic
    fun findRecipe(rawItemId: Int): CookingDefinition? = recipes.firstOrNull { it.rawItemId == rawItemId }

    @JvmStatic
    fun recipeByIndex(index: Int): CookingDefinition? = recipes.getOrNull(index)
}
