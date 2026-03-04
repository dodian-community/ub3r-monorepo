package net.dodian.uber.game.runtime.sync.player.root

data class PlayerInfoLocalSetDiff(
    val removals: IntArray,
    val retains: IntArray,
    val additions: IntArray,
    val changedRetained: IntArray,
    val requiresRebuild: Boolean,
) {
    val isEmpty: Boolean
        get() = removals.isEmpty() && retains.isEmpty() && additions.isEmpty() && changedRetained.isEmpty()
}
