package net.dodian.uber.game.model

import net.dodian.uber.game.model.chunk.Chunk
import net.dodian.utilities.Misc
import kotlin.jvm.JvmName

class Position @JvmOverloads constructor(x: Int = 2611, y: Int = 3093, z: Int = 0) {
    private var packed: Int = pack(x, y, z)

    @get:JvmName("getXValue")
    val x: Int
        get() = getX()

    @get:JvmName("getYValue")
    val y: Int
        get() = getY()

    @get:JvmName("getZValue")
    @set:JvmName("setZValue")
    var z: Int
        get() = getZ()
        set(value) = setZ(value)

    fun getX(): Int = unpackCoordinate(packed shr X_SHIFT)

    fun getY(): Int = unpackCoordinate(packed shr Y_SHIFT)

    fun getZ(): Int = packed and Z_MASK

    fun setZ(z: Int) {
        packed = pack(getX(), getY(), z)
    }

    fun getDistance(position: Position): Double {
        val difX = kotlin.math.abs(getX() - position.getX())
        val difY = kotlin.math.abs(getY() - position.getY())
        return kotlin.math.sqrt((difX * difX + difY * difY).toDouble())
    }

    fun isWithinRange(position: Position, threshold: Double): Boolean = getDistance(position) <= threshold

    fun moveTo(x: Int, y: Int, z: Int) {
        packed = pack(x, y, z)
    }

    fun moveTo(x: Int, y: Int) {
        moveTo(x, y, getZ())
    }

    fun move(amountX: Int, amountY: Int, amountZ: Int): Position {
        packed = pack(getX() + amountX, getY() + amountY, getZ() + amountZ)
        return this
    }

    fun move(amountX: Int, amountY: Int): Position = move(amountX, amountY, 0)

    fun copy(): Position = Position(getX(), getY(), getZ())

    fun isPerpendicularTo(other: Position): Boolean {
        val delta = Misc.delta(this, other)
        return (delta.getX() != delta.getY() && delta.getX() == 0) || delta.getY() == 0
    }

    fun getLocalX(base: Position): Int = getX() - 8 * base.regionX

    fun getLocalY(base: Position): Int = getY() - 8 * base.regionY

    val localX: Int
        get() = getLocalX(this)

    val localY: Int
        get() = getLocalY(this)

    val regionX: Int
        get() = (getX() shr 3) - 6

    val regionY: Int
        get() = (getY() shr 3) - 6

    val chunk: Chunk
        get() = Chunk(chunkX, chunkY)

    val chunkX: Int
        get() = (getX() shr 3) - 6

    val chunkY: Int
        get() = (getY() shr 3) - 6

    fun withinDistance(other: Position, amount: Int): Boolean {
        val thisX = getX()
        val thisY = getY()
        val thisZ = getZ()
        val otherX = other.getX()
        val otherY = other.getY()
        if (thisZ != other.getZ()) {
            return false
        }
        return kotlin.math.abs(otherX - thisX) <= amount && kotlin.math.abs(otherY - thisY) <= amount
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Position) {
            return false
        }
        return packed == other.packed
    }

    override fun hashCode(): Int = packed

    override fun toString(): String = "x=${getX()} y=${getY()} z=${getZ()}"

    companion object {
        private const val X_BITS = 14
        private const val Y_BITS = 14
        private const val Z_BITS = 4
        private const val Y_SHIFT = Z_BITS
        private const val X_SHIFT = Y_SHIFT + Y_BITS
        private const val X_MASK = (1 shl X_BITS) - 1
        private const val Y_MASK = (1 shl Y_BITS) - 1
        private const val Z_MASK = (1 shl Z_BITS) - 1
        private const val MIN_COORDINATE = -1
        private const val MAX_COORDINATE = X_MASK - 1
        private const val MIN_HEIGHT = 0
        private const val MAX_HEIGHT = Z_MASK

        @JvmStatic
        fun delta(a: Position, b: Position): Position = Position(b.getX() - a.getX(), b.getY() - a.getY())

        private fun pack(x: Int, y: Int, z: Int): Int {
            val encodedX = encodeCoordinate("x", x)
            val encodedY = encodeCoordinate("y", y)
            val encodedZ = encodeHeight(z)
            return (encodedX shl X_SHIFT) or (encodedY shl Y_SHIFT) or encodedZ
        }

        private fun encodeCoordinate(axis: String, value: Int): Int {
            require(value in MIN_COORDINATE..MAX_COORDINATE) { "Position $axis out of range: $value" }
            return value + 1
        }

        private fun unpackCoordinate(encoded: Int): Int = (encoded and X_MASK) - 1

        private fun encodeHeight(value: Int): Int {
            require(value in MIN_HEIGHT..MAX_HEIGHT) { "Position z out of range: $value" }
            return value
        }
    }
}
