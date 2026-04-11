package net.dodian.uber.game.engine.systems.interaction

data class ObjectInteractionPolicy(
    val distanceRule: DistanceRule = DistanceRule.NEAREST_BOUNDARY_CARDINAL,
    val requireMovementSettled: Boolean = false,
    val settleTicks: Int = 0,
) {
    init {
        require(settleTicks >= 0) { "settleTicks must be >= 0" }
    }

    enum class DistanceRule {
        NEAREST_BOUNDARY_CARDINAL,
        NEAREST_BOUNDARY_ANY,
    }

    enum class InteractionType {
        CLICK,
        ITEM_ON_OBJECT,
        MAGIC,
    }

    companion object {
        @JvmField
        val DEFAULT = ObjectInteractionPolicy()
    }
}
