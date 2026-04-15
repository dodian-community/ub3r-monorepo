package net.dodian.uber.game.engine.systems.interaction

data class ObjectInteractionPolicy(
    val distanceRule: DistanceRule = DistanceRule.REACHABLE,
    val requireMovementSettled: Boolean = false,
    val settleTicks: Int = 0,
) {
    init {
        require(settleTicks >= 0) { "settleTicks must be >= 0" }
    }

    enum class DistanceRule {
        NEAREST_BOUNDARY_CARDINAL,
        NEAREST_BOUNDARY_ANY,
        REACHABLE,
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
