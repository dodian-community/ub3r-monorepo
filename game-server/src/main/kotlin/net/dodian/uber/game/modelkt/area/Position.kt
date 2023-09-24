package net.dodian.uber.game.modelkt.area

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt


// TODO: Rewrite - Need to implement

@Suppress("MemberVisibilityCanBePrivate")
data class Position(
    val x: Int,
    val y: Int,
    val height: Int = 0
) {
    val regionCoordinates: RegionCoordinates get() = RegionCoordinates.fromPosition(this)
    val topLeftRegionX: Int get() = x / 8 - 6
    val topLeftRegionY: Int get() = y / 8 - 6

    val localX: Int get() = localX(this)
    val localY: Int get() = localY(this)

    val centralRegionX: Int get() = x / 8
    val centralRegionY: Int get() = y / 8

    constructor(packed: Int) : this(
        packed and 0x7FFF,
        packed shr 15 and 0x7FFF,
        packed ushr 30
    )

    private fun packed(): Int {
        if (height < 0 || height >= HEIGHT_LEVELS)
            error("Height must be between 0 and ${HEIGHT_LEVELS - 1}, received: $height")

        return height shl 30 or (y and 0x7FFF shl 15) or (x and 0x7FFF)
    }

    fun distance(other: Position): Int {
        val deltaX = (x - other.x).toDouble()
        val deltaY = (y - other.y).toDouble()

        return ceil(sqrt(deltaX * deltaX + deltaY * deltaY)).toInt()
    }

    fun localX(base: Position) = x - base.topLeftRegionX * 8
    fun localY(base: Position) = y - base.topLeftRegionY * 8

    fun longestDelta(other: Position): Int {
        val deltaX = abs(x - other.x)
        val deltaY = abs(y - other.y)
        return max(deltaX, deltaY)
    }

    override fun hashCode(): Int {
        return packed()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Position)
            return false

        return packed() == other.packed()
    }

    companion object {
        const val HEIGHT_LEVELS = 4
        const val MAX_DISTANCE = 15

        fun fromPacked(packed: Int) = Position(packed)
    }
}