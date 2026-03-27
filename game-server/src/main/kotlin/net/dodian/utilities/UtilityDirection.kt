package net.dodian.utilities

object UtilityDirection {
    @JvmField val directionDeltaX: ByteArray = byteArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
    @JvmField val directionDeltaY: ByteArray = byteArrayOf(1, 1, 0, -1, -1, -1, 0, 1)
    @JvmField val xlateDirectionToClient: ByteArray = byteArrayOf(1, 2, 4, 7, 6, 5, 3, 0)

    @JvmStatic
    fun direction(dx: Int, dy: Int): Int {
        if (dx < 0) {
            return when {
                dy < 0 -> 5
                dy > 0 -> 0
                else -> 3
            }
        } else if (dx > 0) {
            return when {
                dy < 0 -> 7
                dy > 0 -> 2
                else -> 4
            }
        }
        return when {
            dy < 0 -> 6
            dy > 0 -> 1
            else -> -1
        }
    }

    @JvmStatic
    fun direction(
        srcX: Int,
        srcY: Int,
        destX: Int,
        destY: Int,
    ): Int {
        val dx = destX - srcX
        val dy = destY - srcY
        if (dx < 0) {
            return if (dy < 0) {
                if (dx < dy) 11 else if (dx > dy) 9 else 10
            } else if (dy > 0) {
                if (-dx < dy) 15 else if (-dx > dy) 13 else 14
            } else {
                12
            }
        } else if (dx > 0) {
            return if (dy < 0) {
                if (dx < -dy) 7 else if (dx > -dy) 5 else 6
            } else if (dy > 0) {
                if (dx < dy) 1 else if (dx > dy) 3 else 2
            } else {
                4
            }
        }
        return if (dy < 0) 8 else if (dy > 0) 0 else -1
    }
}
