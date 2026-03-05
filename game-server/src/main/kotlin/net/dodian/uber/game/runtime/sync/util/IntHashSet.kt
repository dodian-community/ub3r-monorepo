package net.dodian.uber.game.runtime.sync.util

import java.util.Arrays
import java.util.function.IntConsumer

class IntHashSet(initialCapacity: Int) {
    private var keys: IntArray
    private var size = 0
    private var threshold: Int
    private var mask: Int

    init {
        val capacity = capacityFor(initialCapacity)
        keys = IntArray(capacity)
        mask = capacity - 1
        threshold = maxOf(1, (capacity * LOAD_FACTOR).toInt())
    }

    fun clear() {
        Arrays.fill(keys, 0)
        size = 0
    }

    fun size(): Int = size

    fun add(key: Int): Boolean {
        val stored = key + 1
        if (size + 1 > threshold) {
            resize(keys.size shl 1)
        }
        var index = mix(stored) and mask
        while (true) {
            val existing = keys[index]
            if (existing == 0) {
                keys[index] = stored
                size++
                return true
            }
            if (existing == stored) {
                return false
            }
            index = (index + 1) and mask
        }
    }

    fun contains(key: Int): Boolean {
        val stored = key + 1
        var index = mix(stored) and mask
        while (true) {
            val existing = keys[index]
            if (existing == 0) {
                return false
            }
            if (existing == stored) {
                return true
            }
            index = (index + 1) and mask
        }
    }

    fun forEach(consumer: IntConsumer) {
        for (stored in keys) {
            if (stored != 0) {
                consumer.accept(stored - 1)
            }
        }
    }

    private fun resize(newCapacity: Int) {
        val oldKeys = keys
        keys = IntArray(newCapacity)
        mask = newCapacity - 1
        threshold = maxOf(1, (newCapacity * LOAD_FACTOR).toInt())
        size = 0
        for (stored in oldKeys) {
            if (stored != 0) {
                add(stored - 1)
            }
        }
    }

    private companion object {
        private const val LOAD_FACTOR = 0.5f

        private fun capacityFor(initialCapacity: Int): Int {
            val desired = maxOf(2, (initialCapacity / LOAD_FACTOR).toInt() + 1)
            var capacity = 1
            while (capacity < desired) {
                capacity = capacity shl 1
            }
            return capacity
        }

        private fun mix(value: Int): Int {
            var x = value
            x = x xor (x ushr 16)
            x *= 0x7feb352d
            x = x xor (x ushr 15)
            x *= 0x846ca68b.toInt()
            x = x xor (x ushr 16)
            return x
        }
    }
}
