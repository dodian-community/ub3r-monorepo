package net.dodian.uber.game.engine.sync.playerinfo.state

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
