package net.dodian.uber.game.skills.smithing

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
