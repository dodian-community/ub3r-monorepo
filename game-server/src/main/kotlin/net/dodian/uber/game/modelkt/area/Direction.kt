package net.dodian.uber.game.modelkt.area

@Suppress("unused", "MemberVisibilityCanBePrivate")
enum class Direction(private val intValue: Int) {
    NONE(-1),
    NORTH_WEST(0),
    NORTH(1),
    NORTH_EAST(2),
    WEST(3),
    EAST(4),
    SOUTH_WEST(5),
    SOUTH(6),
    SOUTH_EAST(7);

    val opposite: Direction
        get() = when (this) {
            NORTH -> SOUTH
            SOUTH -> NORTH
            EAST -> WEST
            WEST -> EAST
            NORTH_WEST -> SOUTH_EAST
            NORTH_EAST -> SOUTH_WEST
            SOUTH_WEST -> NORTH_EAST
            SOUTH_EAST -> NORTH_WEST
            else -> NONE
        }

    val diagonalComponents: Array<Direction>
        get() = when (this) {
            NORTH_EAST -> arrayOf(NORTH, EAST)
            NORTH_WEST -> arrayOf(NORTH, WEST)
            SOUTH_EAST -> arrayOf(SOUTH, EAST)
            SOUTH_WEST -> arrayOf(SOUTH, WEST)
            else -> error("$this is not a diagonal direction.")
        }

    val deltaX: Int
        get() = when (this) {
            SOUTH_EAST, NORTH_EAST, EAST -> 1
            SOUTH_WEST, NORTH_WEST, WEST -> -1
            else -> 0
        }

    val deltaY: Int
        get() = when (this) {
            NORTH_WEST, NORTH_EAST, NORTH -> 1
            SOUTH_WEST, SOUTH_EAST, SOUTH -> -1
            else -> 0
        }

    val isDiagonal: Boolean get() = WNES_DIAGONAL.contains(this)

    val orientationInt get() = when(this) {
        WEST, NORTH_WEST -> 0
        NORTH, NORTH_EAST -> 1
        EAST, SOUTH_EAST -> 2
        SOUTH, SOUTH_WEST -> 3
        else -> error("$this does not have an orientation-integer.")
    }

    fun toInt() = this.intValue

    companion object {
        val EMPTY_DIRECTION_ARRAY: Array<Direction?> = arrayOfNulls<Direction>(0)
        val NESW: Array<Direction> = arrayOf(NORTH, EAST, SOUTH, WEST)
        val WNES: Array<Direction> = arrayOf(WEST, NORTH, EAST, SOUTH)
        val WNES_DIAGONAL: Array<Direction> = arrayOf(NORTH_WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST)

        fun between(current: Position, next: Position): Direction {
            val deltaX = next.x - current.x
            val deltaY = next.y - current.y

            return fromDeltas(deltaX, deltaY)
        }

        private fun Int.direction(vararg directions: Pair<Int, Direction>) = mapOf(*directions)[this]
            ?: error("'deltaX' value needs to be between -1 and 1.")

        fun fromDeltas(deltaX: Int, deltaY: Int) = when (deltaY) {
            1 -> deltaX.direction(
                1 to NORTH_EAST,
                0 to NORTH,
                -1 to NORTH_WEST
            )

            -1 -> deltaX.direction(
                1 to SOUTH_EAST,
                0 to SOUTH,
                -1 to SOUTH_WEST
            )

            0 -> deltaX.direction(
                1 to EAST,
                0 to NONE,
                -1 to WEST
            )

            else -> error("'deltaY' value needs to be between -1 and 1.")
        }
    }
}