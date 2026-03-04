package net.dodian.uber.game.runtime.interaction

data class ObjectInteractionPolicy(
    val distanceRule: DistanceRule = DistanceRule.LEGACY_OBJECT_DISTANCE,
    val requireMovementSettled: Boolean = false,
    val settleTicks: Int = 0,
) {
    init {
        require(settleTicks >= 0) { "settleTicks must be >= 0" }
    }

    enum class DistanceRule {
        LEGACY_OBJECT_DISTANCE,
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
