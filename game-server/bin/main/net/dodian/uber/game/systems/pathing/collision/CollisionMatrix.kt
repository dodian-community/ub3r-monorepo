package net.dodian.uber.game.systems.pathing.collision

class CollisionMatrix {
    private val tiles = HashMap<Long, Int>()

    fun apply(update: CollisionUpdate) {
        for (dx in 0 until update.width) {
            for (dy in 0 until update.height) {
                if (update.remove) {
                    clear(update.x + dx, update.y + dy, update.z, update.flags)
                } else {
                    flag(update.x + dx, update.y + dy, update.z, update.flags)
                }
            }
        }
    }

    fun flag(x: Int, y: Int, z: Int, flags: Int) {
        if (flags == 0) {
            return
        }

        val key = key(x, y, z)
        tiles[key] = tiles.getOrDefault(key, 0) or flags
    }

    fun clear(x: Int, y: Int, z: Int, flags: Int) {
        if (flags == 0) {
            return
        }

        val key = key(x, y, z)
        val updated = (tiles[key] ?: 0) and flags.inv()
        if (updated == 0) {
            tiles.remove(key)
        } else {
            tiles[key] = updated
        }
    }

    fun getFlags(x: Int, y: Int, z: Int): Int = tiles[key(x, y, z)] ?: 0

    fun hasFlags(x: Int, y: Int, z: Int, flags: Int): Boolean {
        return getFlags(x, y, z) and flags != 0
    }

    fun hasAllFlags(x: Int, y: Int, z: Int, flags: Int): Boolean {
        if (flags == 0) {
            return true
        }
        return getFlags(x, y, z) and flags == flags
    }

    fun clearAll() {
        tiles.clear()
    }

    private fun key(x: Int, y: Int, z: Int): Long {
        return ((z.toLong() and 0x3L) shl 42) or
            ((x.toLong() and 0x1FFFFFL) shl 21) or
            (y.toLong() and 0x1FFFFFL)
    }
}
