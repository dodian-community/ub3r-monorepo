package net.dodian.uber.game.engine.systems.interaction

import net.dodian.uber.game.model.entity.Entity

/** Footprint-aware reach classification for non-combat entity interactions. */
object EntityInteractionReach {
    @JvmStatic
    fun resolve(actor: Entity?, target: Entity?, distance: Int): EntityReachResult {
        if (actor == null || target == null) return EntityReachResult.INVALID_TARGET
        if (actor.position.z != target.position.z) return EntityReachResult.DIFFERENT_PLANE

        val actorSize = actor.size.coerceAtLeast(1)
        val targetSize = target.size.coerceAtLeast(1)
        if (overlaps(
                actor.position.x,
                actor.position.y,
                actorSize,
                target.position.x,
                target.position.y,
                targetSize,
            )
        ) {
            return EntityReachResult.OVERLAPPING
        }

        val range = distance.coerceAtLeast(0)
        val expandedActorX = actor.position.x - range
        val expandedActorY = actor.position.y - range
        val expandedActorSize = actorSize + range * 2
        return if (
            overlaps(
                expandedActorX,
                expandedActorY,
                expandedActorSize,
                target.position.x,
                target.position.y,
                targetSize,
            )
        ) {
            EntityReachResult.REACHED
        } else {
            EntityReachResult.OUT_OF_RANGE
        }
    }

    private fun overlaps(
        firstX: Int,
        firstY: Int,
        firstSize: Int,
        secondX: Int,
        secondY: Int,
        secondSize: Int,
    ): Boolean =
        firstX < secondX + secondSize &&
            secondX < firstX + firstSize &&
            firstY < secondY + secondSize &&
            secondY < firstY + firstSize
}

enum class EntityReachResult {
    REACHED,
    OVERLAPPING,
    OUT_OF_RANGE,
    DIFFERENT_PLANE,
    INVALID_TARGET,
}
