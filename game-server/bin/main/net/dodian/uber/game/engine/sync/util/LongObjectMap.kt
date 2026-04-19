package net.dodian.uber.game.engine.sync.util

class LongObjectMap<V : Any>(
    initialCapacity: Int = 16,
) {
    private var keys = LongArray(capacityFor(initialCapacity))
    private var values = arrayOfNulls<Any>(keys.size)
    private var size = 0
    private var resizeThreshold = (keys.size * LOAD_FACTOR).toInt().coerceAtLeast(1)

    operator fun get(rawKey: Long): V? {
        val key = rawKey + 1L
        var index = indexFor(key, keys.size)
        while (true) {
            val stored = keys[index]
            if (stored == 0L) {
                return null
            }
            if (stored == key) {
                @Suppress("UNCHECKED_CAST")
                return values[index] as V
            }
            index = (index + 1) and (keys.size - 1)
        }
    }

    fun put(rawKey: Long, value: V): V? {
        val key = rawKey + 1L
        if (size + 1 > resizeThreshold) {
            resize(keys.size shl 1)
        }
        return insert(key, value, keys, values)
    }

    fun getOrPut(
        rawKey: Long,
        supplier: () -> V,
    ): V {
        get(rawKey)?.let { return it }
        val value = supplier()
        put(rawKey, value)
        return value
    }

    fun clear() {
        keys.fill(0L)
        values.fill(null)
        size = 0
    }

    private fun insert(
        storedKey: Long,
        value: V,
        targetKeys: LongArray,
        targetValues: Array<Any?>,
    ): V? {
        var index = indexFor(storedKey, targetKeys.size)
        while (true) {
            val existing = targetKeys[index]
            if (existing == 0L) {
                targetKeys[index] = storedKey
                targetValues[index] = value
                size++
                return null
            }
            if (existing == storedKey) {
                @Suppress("UNCHECKED_CAST")
                val previous = targetValues[index] as V?
                targetValues[index] = value
                return previous
            }
            index = (index + 1) and (targetKeys.size - 1)
        }
    }

    private fun resize(newCapacity: Int) {
        val newKeys = LongArray(newCapacity)
        val newValues = arrayOfNulls<Any>(newCapacity)
        val oldKeys = keys
        val oldValues = values
        keys = newKeys
        values = newValues
        size = 0
        resizeThreshold = (newCapacity * LOAD_FACTOR).toInt().coerceAtLeast(1)
        for (index in oldKeys.indices) {
            val key = oldKeys[index]
            if (key != 0L) {
                @Suppress("UNCHECKED_CAST")
                insert(key, oldValues[index] as V, newKeys, newValues)
            }
        }
    }

    private companion object {
        private const val LOAD_FACTOR = 0.5f

        private fun capacityFor(initialCapacity: Int): Int {
            var capacity = 1
            val desired = (initialCapacity / LOAD_FACTOR).toInt().coerceAtLeast(2)
            while (capacity < desired) {
                capacity = capacity shl 1
            }
            return capacity
        }

        private fun indexFor(
            key: Long,
            length: Int,
        ): Int {
            val mixed = key xor (key ushr 33) xor (key ushr 17)
            return mixed.toInt() and (length - 1)
        }
    }
}
