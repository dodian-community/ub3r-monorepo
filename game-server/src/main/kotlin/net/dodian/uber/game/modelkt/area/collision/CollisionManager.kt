package net.dodian.uber.game.modelkt.area.collision

import net.dodian.uber.game.modelkt.area.Direction
import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.game.modelkt.area.RegionRepository
import net.dodian.uber.game.modelkt.entity.EntityType

class CollisionManager(
    val regions: RegionRepository
) {

    fun isTraversable(position: Position, entityType: EntityType, direction: Direction): Boolean {
        var next = position.step(1, direction)
        var region = regions.fromPosition(next)

        if (!region.isTraversable(next, entityType, direction))
            return false

        if (!direction.isDiagonal)
            return true

        direction.diagonalComponents.forEach { component ->
            next = position.step(1, component)

            if (!region.contains(next))
                region = regions.fromPosition(next)

            if (!region.isTraversable(next, entityType, component))
                return false
        }

        return true
    }
}