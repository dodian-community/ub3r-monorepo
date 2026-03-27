package net.dodian.uber.game.runtime.sync.player.pool

import net.dodian.uber.game.Constants

class PlayerSyncScratchPool(
    maxPlayers: Int = Constants.maxPlayers + 1,
    maxLocals: Int = 255,
) {
    private val marked = BooleanArray(maxPlayers.coerceAtLeast(1))
    private val current = BooleanArray(maxPlayers.coerceAtLeast(1))
    private val touchedMarked = IntArray(maxPlayers.coerceAtLeast(1))
    private val touchedCurrent = IntArray(maxPlayers.coerceAtLeast(1))

    val desiredSlots = IntArray(maxLocals)
    val additions = IntArray(maxLocals)
    val reinserts = IntArray(maxLocals)
    val removals = IntArray(maxLocals)
    val retains = IntArray(maxLocals)
    val changedRetained = IntArray(maxLocals)
    val candidateSlots = IntArray(maxPlayers.coerceAtLeast(1))
    val sortedCandidateSlots = IntArray(maxPlayers.coerceAtLeast(1))
    val sortedCandidateDistances = IntArray(maxPlayers.coerceAtLeast(1))

    private var touchedMarkedCount = 0
    private var touchedCurrentCount = 0

    fun reset() {
        for (index in 0 until touchedMarkedCount) {
            marked[touchedMarked[index]] = false
        }
        for (index in 0 until touchedCurrentCount) {
            current[touchedCurrent[index]] = false
        }
        touchedMarkedCount = 0
        touchedCurrentCount = 0
    }

    fun mark(slot: Int) {
        if (slot < 0 || slot >= marked.size || marked[slot]) {
            return
        }
        marked[slot] = true
        touchedMarked[touchedMarkedCount++] = slot
    }

    fun isMarked(slot: Int): Boolean = slot >= 0 && slot < marked.size && marked[slot]

    fun markCurrent(slot: Int) {
        if (slot < 0 || slot >= current.size || current[slot]) {
            return
        }
        current[slot] = true
        touchedCurrent[touchedCurrentCount++] = slot
    }

    fun isCurrent(slot: Int): Boolean = slot >= 0 && slot < current.size && current[slot]
}
