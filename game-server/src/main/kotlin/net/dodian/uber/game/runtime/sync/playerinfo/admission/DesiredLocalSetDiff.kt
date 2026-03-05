package net.dodian.uber.game.runtime.sync.playerinfo.admission

/**
 * Scratch-backed diff views. Arrays are reused; use the corresponding *Count fields
 * to know how many entries are valid.
 */
data class DesiredLocalSetDiff(
    val removals: IntArray,
    val removalsCount: Int,
    val retains: IntArray,
    val retainsCount: Int,
    val additions: IntArray,
    val additionsCount: Int,
    val changedRetained: IntArray,
    val changedRetainedCount: Int,
    val desiredCount: Int,
    val desiredSaturated: Boolean,
)

