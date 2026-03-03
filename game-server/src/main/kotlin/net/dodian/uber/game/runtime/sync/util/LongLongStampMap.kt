package net.dodian.uber.game.runtime.sync.util

/**
 * Allocation-light open-addressing map for `Long -> Long`.
 *
 * Game-thread owned; not thread-safe.
 *
 * Uses `0L` as the empty sentinel, so callers must ensure keys are never `0L`
 * (e.g. by packing and then adding 1).
 */
class LongLongStampMap(
    initialCapacity: Int = 128,
) {
    private var keys = LongArray(nextPow2(initialCapacity.coerceAtLeast(4)))
    private var values = LongArray(keys.size)
    private var mask = keys.size - 1
    private var size = 0

    fun put(key: Long, value: Long) {
        require(key != 0L) { "Key must be non-zero (0 is reserved as the empty sentinel)" }
        if ((size + 1) * 2 >= keys.size) {
            rehash(keys.size * 2)
        }
        insert(key, value)
    }

    fun getOrZero(key: Long): Long {
        if (key == 0L) return 0L
        var idx = mix(key) and mask.toLong()
        while (true) {
            val k = keys[idx.toInt()]
            if (k == 0L) return 0L
            if (k == key) return values[idx.toInt()]
            idx = (idx + 1) and mask.toLong()
        }
    }

    fun clear() {
        // Avoid per-tick clearing costs; this is mainly for tests / lifecycle resets.
        keys.fill(0L)
        values.fill(0L)
        size = 0
    }

    private fun insert(key: Long, value: Long) {
        var idx = mix(key) and mask.toLong()
        while (true) {
            val k = keys[idx.toInt()]
            if (k == 0L) {
                keys[idx.toInt()] = key
                values[idx.toInt()] = value
                size++
                return
            }
            if (k == key) {
                values[idx.toInt()] = value
                return
            }
            idx = (idx + 1) and mask.toLong()
        }
    }

    private fun rehash(newCapacity: Int) {
        val oldKeys = keys
        val oldValues = values
        keys = LongArray(nextPow2(newCapacity.coerceAtLeast(4)))
        values = LongArray(keys.size)
        mask = keys.size - 1
        size = 0
        for (i in oldKeys.indices) {
            val k = oldKeys[i]
            if (k != 0L) {
                insert(k, oldValues[i])
            }
        }
    }

    private fun mix(x: Long): Long {
        var z = x
        z = (z xor (z ushr 33)) * -0xae502812aa7333L
        z = (z xor (z ushr 33)) * -0x3b314601e57a13adL
        z = z xor (z ushr 33)
        return z
    }

    private fun nextPow2(value: Int): Int {
        var v = value
        v--
        v = v or (v ushr 1)
        v = v or (v ushr 2)
        v = v or (v ushr 4)
        v = v or (v ushr 8)
        v = v or (v ushr 16)
        v++
        return v
    }
}

