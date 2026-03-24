package net.dodian.uber.game.runtime.action

data class ProductionRequest(
    val skillId: Int,
    val productId: Int,
    val amountPerCycle: Int,
    val primaryItemId: Int,
    val secondaryItemId: Int,
    val experiencePerUnit: Int,
    val animationId: Int,
    val tickDelay: Int,
    val completionMessage: String = "",
    val mode: ProductionMode = ProductionMode.GENERIC,
)

data class PendingProductionSelection(
    val request: ProductionRequest,
    val interfaceModelZoom: Int = 190,
    val titleLineBreaks: Int = 5,
)

data class ActiveProductionSelection(
    val request: ProductionRequest,
    val remainingCycles: Int,
)

enum class ProductionMode {
    GENERIC,
    SUPER_COMBAT,
    OVERLOAD,
    CHARGED_ORB,
    MOLTEN_GLASS,
}
