package net.dodian.uber.game.content.skills.smithing

data class SmithingRequest(
    val tierId: Int,
    val product: SmithingProduct,
    val amount: Int,
    val barId: Int,
    val anvilX: Int,
    val anvilY: Int,
)
