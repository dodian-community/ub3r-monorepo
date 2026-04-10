package net.dodian.uber.game.engine.sync.playerinfo.admission

data class DesiredLocalSet(
    val slots: IntArray,
    val count: Int,
    val signature: Int,
    val isSaturated: Boolean,
)
