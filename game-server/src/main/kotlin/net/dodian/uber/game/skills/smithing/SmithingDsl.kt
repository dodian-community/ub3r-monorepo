package net.dodian.uber.game.skills.smithing

class SmeltingRecipeBuilder internal constructor(
    private val barId: Int,
) {
    private var levelRequired: Int = 1
    private var experience: Int = 0
    private var successChancePercent: Int = 100
    private var failureMessage: String? = null
    private val ores = ArrayList<OreRequirement>()

    fun level(level: Int) {
        levelRequired = level
    }

    fun xp(experience: Int) {
        this.experience = experience
    }

    fun ore(itemId: Int, amount: Int = 1) {
        ores += OreRequirement(itemId, amount)
    }

    fun successChance(percent: Int) {
        successChancePercent = percent
    }

    fun failureMessage(message: String) {
        failureMessage = message
    }

    fun build(): SmeltingRecipe {
        return SmeltingRecipe(
            barId = barId,
            levelRequired = levelRequired,
            experience = experience,
            oreRequirements = ores.toList(),
            successChancePercent = successChancePercent,
            failureMessage = failureMessage,
        )
    }
}

class SmeltingRecipesBuilder {
    private val recipes = ArrayList<SmeltingRecipe>()

    fun bar(barId: Int, block: SmeltingRecipeBuilder.() -> Unit) {
        val builder = SmeltingRecipeBuilder(barId)
        builder.block()
        recipes += builder.build()
    }

    fun build(): List<SmeltingRecipe> = recipes
}

fun smeltingRecipes(block: SmeltingRecipesBuilder.() -> Unit): List<SmeltingRecipe> {
    val builder = SmeltingRecipesBuilder()
    builder.block()
    return builder.build()
}
