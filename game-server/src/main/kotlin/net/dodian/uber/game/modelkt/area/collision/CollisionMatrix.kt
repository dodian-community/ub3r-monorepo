package net.dodian.uber.game.modelkt.area.collision

import net.dodian.uber.game.modelkt.area.Direction
import net.dodian.uber.game.modelkt.area.Direction.*
import net.dodian.uber.game.modelkt.area.collision.CollisionFlag.Companion.ofDirection
import net.dodian.uber.game.modelkt.entity.EntityType
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

// TODO: Rewrite - Need to implement

@Suppress("MemberVisibilityCanBePrivate", "unused")
class CollisionMatrix(
    val width: Int,
    val length: Int
) {
    val matrix: Array<Short> = Array<Short>(width * length) { ALL_ALLOWED }

    fun flag(x: Int, y: Int, flag: CollisionFlag) {
        matrix[indexOf(x, y)] = matrix[indexOf(x, y)] or flag.asShort
    }

    fun get(x: Int, y: Int) = (matrix[indexOf(x, y)] and (0xFFFF).toShort()).toInt()
    fun set(x: Int, y: Int, flag: CollisionFlag) = set(x, y, flag.asShort)
    fun set(x: Int, y: Int, value: Short) {
        matrix[indexOf(x, y)] = value
    }

    fun clear(x: Int, y: Int, flag: CollisionFlag) = set(x, y, (matrix[indexOf(x, y)] and flag.asShort.inv()))

    fun block(x: Int, y: Int, impenetrable: Boolean) = set(x, y, if (impenetrable) ALL_BLOCKED else MOBS_BLOCKED)
    fun block(x: Int, y: Int) = block(x, y, true)

    fun isAllFlagsSet(x: Int, y: Int, vararg flags: CollisionFlag): Boolean {
        flags.forEach { flag ->
            if (!isFlagged(x, y, flag))
                return false
        }

        return true
    }

    fun isAnyFlagsSet(x: Int, y: Int, vararg flags: CollisionFlag): Boolean {
        flags.forEach { flag ->
            if (isFlagged(x, y, flag))
                return true
        }

        return false
    }

    fun indexOf(x: Int, y: Int) = when {
        x < 0 || x >= width -> error("X coordinate must be between 0 and $width, received: $x")
        y < 0 || y >= length -> error("X coordinate must be between 0 and $length, received: $y")
        else -> y * width + x
    }

    fun reset(x: Int, y: Int) = set(x, y, ALL_ALLOWED)
    fun reset() {
        for (x in 0 until width) {
            for (y in 0 until width) {
                reset(x, y)
            }
        }
    }

    fun isNotTraversable(x: Int, y: Int, entityType: EntityType, direction: Direction) =
        with(CollisionFlag.forType(entityType)) {
            return@with when (direction) {
                NORTH -> isFlagged(x, y, SOUTH)
                EAST -> isFlagged(x, y, WEST)
                SOUTH -> isFlagged(x, y, NORTH)
                WEST -> isFlagged(x, y, EAST)

                NORTH_WEST -> isFlagged(x, y, SOUTH_EAST, SOUTH, EAST)
                NORTH_EAST -> isFlagged(x, y, SOUTH_WEST, SOUTH, WEST)
                SOUTH_WEST -> isFlagged(x, y, NORTH_EAST, NORTH, EAST)
                SOUTH_EAST -> isFlagged(x, y, NORTH_WEST, NORTH, WEST)

                else -> error("'$direction' is not a recognized direction.")
            }
        }

    private fun Map<Direction, CollisionFlag>.isFlagged(x: Int, y: Int, vararg directions: Direction) =
        directions.any {
            isFlagged(x, y, ofDirection(it))
        }

    fun isFlagged(x: Int, y: Int, flag: CollisionFlag) = (get(x, y).toShort() and flag.asShort) != 0.toShort()

    companion object {
        private const val ALL_ALLOWED: Short = 0b00000000_00000000
        private const val MOBS_BLOCKED: Short = (0b11111111_00000000).toShort()
        private const val ALL_BLOCKED: Short = (0b11111111_11111111).toShort()

        fun createMatrices(count: Int, width: Int, length: Int) =
            Array<CollisionMatrix>(count) { CollisionMatrix(width, length) }
    }
}