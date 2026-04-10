package net.dodian.uber.game.engine.sync.util

import java.util.Arrays
import java.util.function.LongConsumer

class LongHashSet(initialCapacity: Int) {
    private var keys: LongArray
    private var size = 0
    private var threshold: Int
    private var mask: Int

    init {
        val capacity = capacityFor(initialCapacity)
        keys = LongArray(capacity)
        mask = capacity - 1
        threshold = maxOf(1, (capacity * LOAD_FACTOR).toInt())
    }

    fun clear() {
        Arrays.fill(keys, 0L)
        size = 0
    }

    fun size(): Int = size

    fun isEmpty(): Boolean = size == 0

    fun add(key: Long): Boolean {
        val stored = key + 1L
        if (size + 1 > threshold) {
            resize(keys.size shl 1)
        }
        var index = mix(stored) and mask
        while (true) {
            val existing = keys[index]
            if (existing == 0L) {
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

    fun contains(key: Long): Boolean {
        val stored = key + 1L
        var index = mix(stored) and mask
        while (true) {
            val existing = keys[index]
            if (existing == 0L) {
                return false
            }
            if (existing == stored) {
                return true
            }
            index = (index + 1) and mask
        }
    }

    fun forEach(consumer: LongConsumer) {
        for (stored in keys) {
            if (stored != 0L) {
                consumer.accept(stored - 1L)
            }
        }
    }

    private fun resize(newCapacity: Int) {
        val oldKeys = keys
        keys = LongArray(newCapacity)
        mask = newCapacity - 1
        threshold = maxOf(1, (newCapacity * LOAD_FACTOR).toInt())
        size = 0
        for (stored in oldKeys) {
            if (stored != 0L) {
                add(stored - 1L)
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

        private fun mix(value: Long): Int {
            var x = value
            x = x xor (x ushr 33)
            x *= -0xae502812aa7333L
            x = x xor (x ushr 33)
            x *= -0x3b314601e57a13adL
            x = x xor (x ushr 33)
            return x.toInt()
        }
    }
}
