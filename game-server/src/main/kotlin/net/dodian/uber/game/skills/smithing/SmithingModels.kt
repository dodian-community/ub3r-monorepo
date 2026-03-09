package net.dodian.uber.game.skills.smithing

data class OreRequirement(
    val itemId: Int,
    val amount: Int,
)

data class SmeltingRecipe(
    val barId: Int,
    val levelRequired: Int,
    val experience: Int,
    val oreRequirements: List<OreRequirement>,
    val successChancePercent: Int = 100,
    val failureMessage: String? = null,
)

data class FurnaceButtonMapping(
    val buttonId: Int,
    val barId: Int,
    val amount: Int,
)

data class SmeltingButtonSet(
    val displayName: String,
    val barId: Int,
    val oneButtonId: Int,
    val fiveButtonId: Int,
    val tenButtonId: Int,
    val xButtonId: Int,
) {
    fun toMappings(): List<FurnaceButtonMapping> = listOf(
        FurnaceButtonMapping(oneButtonId, barId, 1),
        FurnaceButtonMapping(fiveButtonId, barId, 5),
        FurnaceButtonMapping(tenButtonId, barId, 10),
        FurnaceButtonMapping(xButtonId, barId, 0),
    ).filter { it.buttonId > 0 }
}

data class SmithingProduct(
    val itemId: Int,
    val outputAmount: Int,
    val levelRequired: Int,
    val barsRequired: Int,
    val barCountLineId: Int,
    val itemNameLineId: Int,
)

data class SmithingTier(
    val typeId: Int,
    val displayName: String,
    val barId: Int,
    val products: List<SmithingProduct>,
)

data class ActiveSmithingSelection(
    val tierId: Int,
    val barId: Int,
    val anvilX: Int,
    val anvilY: Int,
)

data class SmithingRequest(
    val tierId: Int,
    val product: SmithingProduct,
    val amount: Int,
    val barId: Int,
    val anvilX: Int,
    val anvilY: Int,
)

data class SmeltingSelection(
    val recipe: SmeltingRecipe,
    val amount: Int,
)

data class SmithingDisplayItem(
    val itemId: Int,
    val amount: Int,
)
