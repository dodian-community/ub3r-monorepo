package net.dodian.uber.game.skills.smithing

data class SmithingFrameEntry(
    val itemId: Int,
    val outputAmount: Int,
    val levelRequired: Int,
    val barsRequired: Int,
    val barCountLineId: Int,
    val itemNameLineId: Int,
)
