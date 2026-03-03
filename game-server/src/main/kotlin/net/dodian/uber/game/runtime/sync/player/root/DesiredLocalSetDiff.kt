package net.dodian.uber.game.runtime.sync.player.root

data class DesiredLocalSetDiff(
    val removals: IntArray,
    val retains: IntArray,
    val additions: IntArray,
    val changedRetained: IntArray,
    val desiredCount: Int,
    val desiredSaturated: Boolean,
)
