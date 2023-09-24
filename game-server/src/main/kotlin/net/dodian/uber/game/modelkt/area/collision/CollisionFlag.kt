package net.dodian.uber.game.modelkt.area.collision

import net.dodian.uber.game.modelkt.area.Direction
import net.dodian.uber.game.modelkt.area.Direction.*
import net.dodian.uber.game.modelkt.entity.EntityType

@Suppress("MemberVisibilityCanBePrivate")
enum class CollisionFlag(val bit: Int, val asShort: Short = (1 shl bit).toShort()) {
    MOB_NORTH_WEST(1),
    MOB_NORTH(2),
    MOB_NORTH_EAST(3),
    MOB_EAST(4),
    MOB_SOUTH_EAST(5),
    MOB_SOUTH(6),
    MOB_SOUTH_WEST(7),
    MOB_WEST(8),

    PROJECTILE_NORTH_WEST(9),
    PROJECTILE_NORTH(10),
    PROJECTILE_NORTH_EAST(11),
    PROJECTILE_EAST(12),
    PROJECTILE_SOUTH_EAST(13),
    PROJECTILE_SOUTH(14),
    PROJECTILE_SOUTH_WEST(15),
    PROJECTILE_WEST(16);

    companion object {

        fun Map<Direction, CollisionFlag>.ofDirection(direction: Direction) = this[direction]
            ?: error("There are no collision flags for the direction $direction.")

        val mobs: Map<Direction, CollisionFlag>
            get() = mapOf(
                NORTH_WEST to MOB_NORTH_WEST,
                NORTH to MOB_NORTH,
                NORTH_EAST to MOB_NORTH_EAST,
                EAST to MOB_EAST,
                SOUTH_EAST to MOB_SOUTH_EAST,
                SOUTH to MOB_SOUTH,
                SOUTH_WEST to MOB_SOUTH_WEST,
                WEST to MOB_WEST
            )

        val projectiles: Map<Direction, CollisionFlag>
            get() = mapOf(
                NORTH_WEST to PROJECTILE_NORTH_WEST,
                NORTH to PROJECTILE_NORTH,
                NORTH_EAST to PROJECTILE_NORTH_EAST,
                EAST to PROJECTILE_EAST,
                SOUTH_EAST to PROJECTILE_SOUTH_EAST,
                SOUTH to PROJECTILE_SOUTH,
                SOUTH_WEST to PROJECTILE_SOUTH_WEST,
                WEST to PROJECTILE_WEST
            )

        fun forType(type: EntityType) = when (type == EntityType.PROJECTILE) {
            true -> projectiles
            false -> mobs
        }
    }
}