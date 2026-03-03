package net.dodian.uber.game.runtime.sync.player.root

data class DesiredLocalSet(
    val slots: IntArray,
    val count: Int,
    val signature: Int,
    val isSaturated: Boolean,
)
