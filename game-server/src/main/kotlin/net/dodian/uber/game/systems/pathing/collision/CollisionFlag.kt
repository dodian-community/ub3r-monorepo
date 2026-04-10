package net.dodian.uber.game.systems.pathing.collision

object CollisionFlag {
    const val MOB_NORTH_WEST: Int = 1 shl 0
    const val MOB_NORTH: Int = 1 shl 1
    const val MOB_NORTH_EAST: Int = 1 shl 2
    const val MOB_WEST: Int = 1 shl 3
    const val MOB_EAST: Int = 1 shl 4
    const val MOB_SOUTH_WEST: Int = 1 shl 5
    const val MOB_SOUTH: Int = 1 shl 6
    const val MOB_SOUTH_EAST: Int = 1 shl 7

    const val PROJECTILE_NORTH_WEST: Int = 1 shl 8
    const val PROJECTILE_NORTH: Int = 1 shl 9
    const val PROJECTILE_NORTH_EAST: Int = 1 shl 10
    const val PROJECTILE_WEST: Int = 1 shl 11
    const val PROJECTILE_EAST: Int = 1 shl 12
    const val PROJECTILE_SOUTH_WEST: Int = 1 shl 13
    const val PROJECTILE_SOUTH: Int = 1 shl 14
    const val PROJECTILE_SOUTH_EAST: Int = 1 shl 15

    val MOBS: IntArray =
        intArrayOf(
            MOB_NORTH_WEST,
            MOB_NORTH,
            MOB_NORTH_EAST,
            MOB_WEST,
            MOB_EAST,
            MOB_SOUTH_WEST,
            MOB_SOUTH,
            MOB_SOUTH_EAST,
        )

    val PROJECTILES: IntArray =
        intArrayOf(
            PROJECTILE_NORTH_WEST,
            PROJECTILE_NORTH,
            PROJECTILE_NORTH_EAST,
            PROJECTILE_WEST,
            PROJECTILE_EAST,
            PROJECTILE_SOUTH_WEST,
            PROJECTILE_SOUTH,
            PROJECTILE_SOUTH_EAST,
        )

    const val FULL_MOB_BLOCK: Int =
        MOB_NORTH_WEST or MOB_NORTH or MOB_NORTH_EAST or MOB_WEST or MOB_EAST or MOB_SOUTH_WEST or MOB_SOUTH or MOB_SOUTH_EAST

    const val FULL_PROJECTILE_BLOCK: Int =
        PROJECTILE_NORTH_WEST or PROJECTILE_NORTH or PROJECTILE_NORTH_EAST or PROJECTILE_WEST or PROJECTILE_EAST or PROJECTILE_SOUTH_WEST or PROJECTILE_SOUTH or PROJECTILE_SOUTH_EAST

    const val FULL_BLOCK: Int = FULL_MOB_BLOCK or FULL_PROJECTILE_BLOCK

    fun approachMask(dx: Int, dy: Int): Int =
        when {
            dx == -1 && dy == 1 -> MOB_SOUTH_EAST or MOB_SOUTH or MOB_EAST
            dx == 0 && dy == 1 -> MOB_SOUTH
            dx == 1 && dy == 1 -> MOB_SOUTH_WEST or MOB_SOUTH or MOB_WEST
            dx == -1 && dy == 0 -> MOB_EAST
            dx == 1 && dy == 0 -> MOB_WEST
            dx == -1 && dy == -1 -> MOB_NORTH_EAST or MOB_NORTH or MOB_EAST
            dx == 0 && dy == -1 -> MOB_NORTH
            dx == 1 && dy == -1 -> MOB_NORTH_WEST or MOB_NORTH or MOB_WEST
            else -> 0
        }

    fun singleDirectionFlag(dx: Int, dy: Int, projectile: Boolean = false): Int {
        val flags = if (projectile) PROJECTILES else MOBS
        return when {
            dx == -1 && dy == 1 -> flags[0]
            dx == 0 && dy == 1 -> flags[1]
            dx == 1 && dy == 1 -> flags[2]
            dx == -1 && dy == 0 -> flags[3]
            dx == 1 && dy == 0 -> flags[4]
            dx == -1 && dy == -1 -> flags[5]
            dx == 0 && dy == -1 -> flags[6]
            dx == 1 && dy == -1 -> flags[7]
            else -> 0
        }
    }

    fun fullTile(impenetrable: Boolean = true): Int =
        FULL_MOB_BLOCK or if (impenetrable) FULL_PROJECTILE_BLOCK else 0
}

enum class CollisionDirection(
    val dx: Int,
    val dy: Int,
) {
    NORTH_WEST(-1, 1),
    NORTH(0, 1),
    NORTH_EAST(1, 1),
    WEST(-1, 0),
    EAST(1, 0),
    SOUTH_WEST(-1, -1),
    SOUTH(0, -1),
    SOUTH_EAST(1, -1),
    NONE(0, 0),
    ;

    fun opposite(): CollisionDirection =
        when (this) {
            NORTH_WEST -> SOUTH_EAST
            NORTH -> SOUTH
            NORTH_EAST -> SOUTH_WEST
            WEST -> EAST
            EAST -> WEST
            SOUTH_WEST -> NORTH_EAST
            SOUTH -> NORTH
            SOUTH_EAST -> NORTH_WEST
            NONE -> NONE
        }

    fun isDiagonal(): Boolean = dx != 0 && dy != 0

    companion object {
        val WNES: List<CollisionDirection> = listOf(WEST, NORTH, EAST, SOUTH)
        val WNES_DIAGONAL: List<CollisionDirection> = listOf(NORTH_WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST)

        fun fromDelta(dx: Int, dy: Int): CollisionDirection =
            values().firstOrNull { it.dx == dx && it.dy == dy } ?: NONE

        fun diagonalComponents(direction: CollisionDirection): List<CollisionDirection> =
            when (direction) {
                NORTH_WEST -> listOf(NORTH, WEST)
                NORTH_EAST -> listOf(NORTH, EAST)
                SOUTH_WEST -> listOf(SOUTH, WEST)
                SOUTH_EAST -> listOf(SOUTH, EAST)
                else -> error("Direction $direction is not diagonal.")
            }
    }
}
