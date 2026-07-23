package net.dodian.uber.game.engine.sync.util

/**
 * Allocation-free slot membership for synchronization hot paths.
 *
 * Only words touched since the last clear are reset, which keeps the 16K NPC slot domain cheap
 * when a viewer has at most 255 locals.
 */
class SlotBitSet(capacity: Int) {
    private val words = LongArray((capacity.coerceAtLeast(1) + 63) ushr 6)
    private val touchedWords = IntArray(words.size)
    private var touchedCount = 0

    fun add(slot: Int): Boolean {
        if (slot < 0) return false
        val wordIndex = slot ushr 6
        if (wordIndex !in words.indices) return false
        val bit = 1L shl (slot and 63)
        val previous = words[wordIndex]
        if (previous and bit != 0L) return false
        if (previous == 0L) touchedWords[touchedCount++] = wordIndex
        words[wordIndex] = previous or bit
        return true
    }

    fun contains(slot: Int): Boolean {
        if (slot < 0) return false
        val wordIndex = slot ushr 6
        if (wordIndex !in words.indices) return false
        return words[wordIndex] and (1L shl (slot and 63)) != 0L
    }

    fun clear() {
        for (index in 0 until touchedCount) words[touchedWords[index]] = 0L
        touchedCount = 0
    }
}
