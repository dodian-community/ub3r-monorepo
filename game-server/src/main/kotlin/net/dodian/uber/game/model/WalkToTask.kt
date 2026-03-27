package net.dodian.uber.game.model

class WalkToTask(
    val walkToAction: Action,
    val walkToId: Int,
    val walkToPosition: Position,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is WalkToTask) {
            return false
        }
        return other.walkToAction == walkToAction &&
            other.walkToId == walkToId &&
            other.walkToPosition === walkToPosition
    }

    override fun hashCode(): Int {
        var result = walkToAction.hashCode()
        result = 31 * result + walkToId
        result = 31 * result + System.identityHashCode(walkToPosition)
        return result
    }

    enum class Action {
        OBJECT_FIRST_CLICK,
        OBJECT_SECOND_CLICK,
        OBJECT_THIRD_CLICK,
        OBJECT_FOURTH_CLICK,
        NPC_FIRST_CLICK,
        NPC_SECOND_CLICK,
        NPC_THIRD_CLICK,
        NPC_FOURTH_CLICK,
        ITEM_ON_OBJECT,
        ITEM_ON_NPC,
        ATTACK_NPC,
        ATTACK_PLAYER,
    }
}
