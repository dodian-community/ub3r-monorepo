package net.dodian.uber.game.skills.smithing

data class SmeltingRequest(
    val recipe: SmeltingRecipe,
    val amount: Int,
)
